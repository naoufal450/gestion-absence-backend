package com.ensa.absence.controller;

import com.ensa.absence.domain.entity.Groupe;
import com.ensa.absence.payload.EtudiantResponse;
import com.ensa.absence.payload.GroupeResponse;
import com.ensa.absence.service.GroupeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groupes")
public class GroupeController {
	
	@Autowired
	private GroupeService groupeService;
	
	@GetMapping
	public Iterable<Groupe> getGroupes(){
		return groupeService.getAllGroupes();
	}
	
	@GetMapping("/{groupeId}")
	public Groupe getGroupe(@PathVariable Long groupeId) {
		return groupeService.getGroupeById(groupeId).get();
	}
	
	@GetMapping("/{groupeId}/etudiants")
	public List<EtudiantResponse> getEtudiantsByGroupe(@PathVariable Long groupeId) {
		return groupeService.getEtudiantsByGroupe(groupeId);
	}
	
	@GetMapping("/filiere/{filiereId}")
	public List<GroupeResponse> getGroupesOfFiliere(@PathVariable(value = "filiereId", required = true) Long filierefId){
		return groupeService.getGroupesOfFiliere(filierefId);
	}
	
}
