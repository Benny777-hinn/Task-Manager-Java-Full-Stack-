package com.taskmanager.entity;

import jakarta.persistence.*;

/**
 * A label a user can apply to their tasks (e.g. Work, Study, Personal).
 * Categories belong to exactly one user, so two users can both have a "Work" category.
 */
@Entity
@Table(
        name = "categories",
        // The same user cannot create two categories with the same name.
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "owner_id"})
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    public Category() {
    }

    public Category(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    // ---- getters / setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
