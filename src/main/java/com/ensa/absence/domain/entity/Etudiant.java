package com.ensa.absence.domain.entity;

import com.ensa.absence.domain.audit.ResponsableDateAudit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Class representant un etudiant
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Etudiant extends ResponsableDateAudit implements AppUtilisateur {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	/**
	 * Nom et prenom de l'etudiant
	 */
	@Column(nullable = false)
	private String nom;
	@Column(nullable = false)
	private String prenom;
	
	private String imagePath;

	/**
	 * Objet User coontenant les identifiants de l'étudiant (CNE et numero Apogee)
	 */
	@OneToOne(cascade = CascadeType.ALL)
	private User user;
	
	@ManyToOne
	private Filiere filiere;
	
	/**
	 * Les groupes à lesquel appartient l'étudiant (Groupe de cours, facultativement
	 * un groupe de TD et/ou TP)
	 * Peut appartenir au maximum à un seul groupe de chaque type
	 */
	@ManyToMany(mappedBy = "etudiants", cascade = {CascadeType.MERGE})
	private Set<Groupe> groupes = new HashSet<>();
	
	private boolean archived;

	public Etudiant(String nom, String prenom, String imagePath, User user, Filiere filiere) {
		super();
		this.nom = nom;
		this.prenom = prenom;
		this.imagePath = imagePath;
		this.user = user;
		this.filiere = filiere;
	}

	public Etudiant(String nom, String prenom, String imagePath, User user) {
		super();
		this.nom = nom;
		this.prenom = prenom;
		this.imagePath = imagePath;
		this.user = user;
	}

	@Override
	public int hashCode() {
		return 13;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Etudiant other = (Etudiant) obj;
		return id != null && id.equals(other.getId());
	}
}
