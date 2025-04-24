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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdConsultation() {
        return idConsultation;
    }

    public String getDescription() {
        return description;
    }

    public String getRecommandations() {
        return recommandations;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIdConsultation(int idConsultation) {
        this.idConsultation = idConsultation;
    }

    public void setRecommandations(String recommandations) {
        this.recommandations = recommandations;
    }


}
