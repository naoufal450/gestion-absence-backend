package com.ensa.absence.service.impl;

import com.ensa.absence.domain.entity.Absence;
import com.ensa.absence.payload.AbsenceResponse;
import com.ensa.absence.payload.NewOrModifyAbsenceRequest;
import com.ensa.absence.repository.AbsenceRepository;
import com.ensa.absence.service.AbsenceService;
import com.ensa.absence.utils.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AbsenceServiceImpl implements AbsenceService {

	@Autowired
	private AbsenceRepository absenceRepository;

	@Override
	public List<AbsenceResponse> getAbsencesResponsesOfSeance(Long seanceId) {
		return absenceRepository.findBySeance_Id(seanceId).stream().map(ModelMapper::mapAbsenceToAbsenceResponse)
				.collect(Collectors.toList());
	}

	@Override
	public void modifyAbsencesOfSeance(Long seanceId, List<NewOrModifyAbsenceRequest> request) {
		List<Absence> oldAbsences = absenceRepository.findBySeance_Id(seanceId);
		oldAbsences.forEach(a -> {
			if (request.stream().noneMatch(req -> req.getEtudiantId().compareTo(a.getEtudiant().getId()) == 0)) {
				absenceRepository.delete(a);
			}
		});
	}

}
