package ru.javamentor.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;


@Getter
@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    Long id;

    @Column
    @Setter
    String title;

    @Column
    @Setter
    String content;

    @Column
    final LocalDateTime dateCreated;

    @ManyToMany
    @JoinTable(name = "users_topics", joinColumns = @JoinColumn(name = "topic_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> authors;

    public Topic(String title, String content, Set<User> authors) {
        this.title = title;
        this.content = content;
        this.authors = authors;
        this.dateCreated = LocalDateTime.now(ZoneId.of("GMT"));
    }

}
