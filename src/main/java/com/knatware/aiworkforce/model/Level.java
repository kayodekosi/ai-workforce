package com.knatware.aiworkforce.model;

import jakarta.persistence.*;

/** A seniority level/grade, e.g. "Associate" (rank 1) ... "Director" (rank 5). */
@Entity
@Table(name = "level")
public class Level {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int rank;   // higher = more senior

    public Level() { }
    public Level(String name, int rank) { this.name = name; this.rank = rank; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
}
