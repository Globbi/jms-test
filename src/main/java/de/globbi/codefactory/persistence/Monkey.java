package de.globbi.codefactory.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(schema = "MIRKO")
@Getter @Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class Monkey {

    @Id
    @Column(scale = 19, precision = 0)
    @GeneratedValue(generator = "SEQ_GEN")
    @SequenceGenerator(
            name = "SEQ_GEN",
            sequenceName = "MIRKO.SEQ_MONKEY",
            allocationSize = 1
    )
    private long id;

    @NonNull
    private String message;

    @Override
    public String toString() {
        return message;
    }
}
