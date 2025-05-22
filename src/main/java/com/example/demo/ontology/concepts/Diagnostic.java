package com.example.demo.ontology.concepts;



import jade.content.Concept;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Diagnostic implements Concept {
    private int id;
    private String description;
    private String recommandations;
    private int idConsultation;


    @Override
    public String toString() {
        return "Diagnostic{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", recommandations='" + recommandations + '\'' +
                ", idConsultation=" + idConsultation +
                '}';
    }
}
