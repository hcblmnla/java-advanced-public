package info.kgeorgiy.ja.serov.crawler;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.NewCrawler;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Thread-safe {@link Crawler} implementation. Crawls websites.
 *
 * @author alnmlbch
 */
public class WebCrawler implements NewCrawler {

    private static final double DEFAULT_TIME_SCALE = 1.;

    private static final int DEFAULT_DOWNLOADERS_NUMBER = 10;
    private static final int DEFAULT_EXTRACTORS_NUMBER = 10;
    private static final int DEFAULT_PER_HOST = 10;

    private static final int DEFAULT_DEPTH = 2;

    private static final Collector<CharSequence, ?, String> PRETTY_JOINER = Collectors.joining(", ");

    private final Downloader downloader;
    private final int perHost;

    private final ExecutorService downloadExecutor;
    private final ExecutorService extractExecutor;

    private final ConcurrentMap<String, Semaphore> hosts = new ConcurrentHashMap<>();

    /**
     * Default {@link WebCrawler} constructor.
     *
     * @param downloader  {@link Downloader downloader} implementation
     * @param downloaders max number of downloaders
     * @param extractors  max number of extractors
     * @param perHost     max number of download pages from one host
     */
    public WebCrawler(
        final Downloader downloader,
        final int downloaders,
        final int extractors,
        final int perHost
    ) {
        this.downloader = downloader;
        this.perHost = perHost;
        this.downloadExecutor = Executors.newFixedThreadPool(downloaders);
        this.extractExecutor = Executors.newFixedThreadPool(extractors);
    }

    /**
     * {@link WebCrawler Web crawler} launcher.
     * <p>
     * Downloads website up to specified depth with custom numbers of
     * <ul>
     *     <li>downloaders</li>
     *     <li>extractors</li>
     *     <li>perHost</li>
     * </ul>
     * <p>
     * Usage: {@code WebCrawler url [depth [downloaders [extractors [perHost]]]]}
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        if (args.length < 1) {
            System.err.println("""
                Usage: WebCrawler url [depth [downloaders [extractors [perHost]]]]
                """);
            return;
        }
        final Downloader downloader;
        try {
            downloader = new CachingDownloader(DEFAULT_TIME_SCALE);
        } catch (final IOException e) {
            System.err.println("Failed creation caching downloader with: " + e.getMessage());
            return;
        }
        final Result result;
        try {
            final int downloaders = uintOrDefault(args, 2, DEFAULT_DOWNLOADERS_NUMBER);
            final int extractors = uintOrDefault(args, 3, DEFAULT_EXTRACTORS_NUMBER);
            final int perHost = uintOrDefault(args, 4, DEFAULT_PER_HOST);

            final String url = args[0];
            final int depth = uintOrDefault(args, 1, DEFAULT_DEPTH);

            try (Crawler crawler = new WebCrawler(downloader, downloaders, extractors, perHost)) {
                result = crawler.download(url, depth);
            }
        } catch (final NumberFormatException e) {
            System.err.println("All optional arguments must be a positive integer");
            return;
        }
        System.out.format("""
            Successfully downloaded pages: %s
            Pages downloaded with errors: %s
            """
            .formatted(
                result.getDownloaded().stream().collect(PRETTY_JOINER),
                result.getErrors().entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue().getMessage())
                    .collect(PRETTY_JOINER)
            ));
    }

    private static int uintOrDefault(final String[] args, final int index, final int defaultValue) {
        if (index >= args.length) {
            return defaultValue;
        }
        final int parsed = Integer.parseInt(args[index]);
        if (parsed < 0) {
            throw new NumberFormatException();
        }
        return parsed;
    }

    @Override
    public Result download(final String url, final int depth, final List<String> excludes) {
        try (Crawler tour = new Tour(excludes)) {
            return tour.download(url, depth);
        }
    }

    @Override
    public void close() {
        downloadExecutor.close();
        extractExecutor.close();
    }

    private class Tour implements Crawler {

        private final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        private final ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();

        private final List<String> excludes;

        private Tour(final List<String> excludes) {
            this.excludes = excludes;
        }

        @Override
        public Result download(final String url, final int depth) {
            final Set<String> _ = Collections
                .<Set<String>>nCopies(depth, null)
                .stream()
                .reduce(Set.of(url), this::downloadStep);
            return new Result(List.copyOf(downloaded), errors);
        }

        private Set<String> downloadStep(final Set<String> urls, final Set<String> ignored) {
            final List<CompletableFuture<List<String>>> futures = urls.stream()
                .map(url -> CompletableFuture
                    .supplyAsync(() -> limitedDownload(url), downloadExecutor)
                    .thenApplyAsync(
                        document -> document.map(doc -> {
                            if (errors.containsKey(url)) {
                                return null;
                            }
                            try {
                                return doc.extractLinks();
                            } catch (final IOException e) {
                                return errorT(url, e, null);
                            }
                        }).orElseGet(List::of),
                        extractExecutor
                    )
                )
                .toList();
            final List<List<String>> batches = futures.stream()
                .map(CompletableFuture::join)
                .toList();
            return batches.stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        }

        private Optional<Document> limitedDownload(final String url) {
            if (downloaded.contains(url) || errors.containsKey(url)) {
                return Optional.empty();
            }
            final String host;
            try {
                host = URLUtils.getHost(url);
            } catch (final MalformedURLException e) {
                return errorOptional(url, e);
            }
            if (excludes.stream().anyMatch(host::contains)) {
                return Optional.empty();
            }
            final Semaphore semaphore = hosts.computeIfAbsent(host, _ -> new Semaphore(perHost));
            semaphore.acquireUninterruptibly();
            final Document document;
            try {
                document = downloader.download(url);
            } catch (final IOException e) {
                return errorOptional(url, e);
            } finally {
                semaphore.release();
            }
            downloaded.add(url);
            return Optional.of(document);
        }

        private <O> Optional<O> errorOptional(final String url, final IOException e) {
            return errorT(url, e, Optional.empty());
        }

        private <T> T errorT(final String url, final IOException e, final T defaultValue) {
            errors.put(url, e);
            return defaultValue;
        }

        @Override
        public void close() {
            // nothing to close
        }
    }
}
