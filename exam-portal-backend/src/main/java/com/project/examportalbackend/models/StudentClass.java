package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A student's assignment to a class (category). Composite key (userId, catId).
 * Raw Long FKs per repo style.
 */
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "student_class")
@IdClass(StudentClass.StudentClassId.class)
public class StudentClass {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "cat_id")
    private Long catId;

    public StudentClass(Long userId, Long catId) {
        this.userId = userId;
        this.catId = catId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StudentClassId implements Serializable {
        private Long userId;
        private Long catId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StudentClassId)) return false;
            StudentClassId that = (StudentClassId) o;
            return Objects.equals(userId, that.userId) && Objects.equals(catId, that.catId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, catId);
        }
    }
}
