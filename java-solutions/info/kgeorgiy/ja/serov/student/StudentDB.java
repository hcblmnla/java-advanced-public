package info.kgeorgiy.ja.serov.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements StudentQuery {

    private static final String EMPTY_NAME = "";
    private static final Comparator<Student> COMPARATOR = Student::compareTo;

    private static final Comparator<Student> FULL_COMPARATOR = Comparator
        .comparing(Student::firstName)
        .thenComparing(Student::lastName)
        .thenComparing(COMPARATOR);

    private <T, U, R> R map(
        final Collection<? extends T> collection,
        final Function<? super T, ? extends U> mapper,
        final Collector<? super U, ?, R> collector
    ) {
        return collection.stream()
            .map(mapper)
            .collect(collector);
    }

    private <T, R> List<R> mapToList(
        final Collection<? extends T> collection,
        final Function<? super T, ? extends R> mapper
    ) {
        return map(collection, mapper, Collectors.toUnmodifiableList());
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return mapToList(students, Student::firstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return mapToList(students, Student::lastName);
    }

    @Override
    public List<GroupName> getGroupNames(final List<Student> students) {
        return mapToList(students, Student::groupName);
    }

    private String getFullName(final Student student) {
        return "%s %s".formatted(student.firstName(), student.lastName());
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return mapToList(students, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return map(students, Student::firstName, Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> students) {
        return students.stream()
            .max(COMPARATOR)
            .map(Student::firstName)
            .orElse(EMPTY_NAME);
    }

    private <T> List<T> sortBy(
        final Collection<? extends T> collection,
        final Comparator<? super T> comparator
    ) {
        return collection.stream()
            .sorted(comparator)
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return sortBy(students, COMPARATOR);
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortBy(students, FULL_COMPARATOR);
    }

    private <T> List<Student> findStudentsBy(
        final Collection<Student> students,
        final T specifier,
        final Function<Student, ? extends T> extractor
    ) {
        return students.stream()
            .filter(student -> Objects.equals(
                extractor.apply(student),
                specifier
            ))
            .sorted(FULL_COMPARATOR)
            .toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(
        final Collection<Student> students,
        final String name
    ) {
        return findStudentsBy(students, name, Student::firstName);
    }

    @Override
    public List<Student> findStudentsByLastName(
        final Collection<Student> students,
        final String name
    ) {
        return findStudentsBy(students, name, Student::lastName);
    }

    @Override
    public List<Student> findStudentsByGroup(
        final Collection<Student> students,
        final GroupName group
    ) {
        return findStudentsBy(students, group, Student::groupName);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(
        final Collection<Student> students,
        final GroupName group
    ) {
        return students.stream()
            .filter(student -> student.groupName() == group)
            .collect(Collectors.toMap(
                Student::lastName,
                Student::firstName,
                BinaryOperator.minBy(Comparator.naturalOrder())
            ));
    }
}
