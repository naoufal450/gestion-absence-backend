package com.ensa.absence.service.impl;

import com.ensa.absence.domain.entity.Etudiant;
import com.ensa.absence.domain.entity.Filiere;
import com.ensa.absence.domain.entity.Groupe;
import com.ensa.absence.domain.entity.User;
import com.ensa.absence.domain.enums.GroupeCategorie;
import com.ensa.absence.domain.enums.RoleName;
import com.ensa.absence.exception.EtudiantsExcelFileHasWrogFormat;
import com.ensa.absence.exception.ExcelFileCellNotKnown;
import com.ensa.absence.payload.AbsenceResponse;
import com.ensa.absence.payload.CreateEtudiantRequest;
import com.ensa.absence.payload.EtudiantResponse;
import com.ensa.absence.repository.*;
import com.ensa.absence.service.EtudiantService;
import com.ensa.absence.service.GroupeService;
import com.ensa.absence.service.UserService;
import com.ensa.absence.utils.ModelMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EtudiantServiceImpl implements EtudiantService {

	@Autowired
	private EtudiantRepository etudiantRepository;

	@Autowired
	private GroupeService groupeService;

	@Autowired
    private GroupeRepository groupeRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private FiliereRepository filiereRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private AbsenceRepository absenceRepository;

	@Override
	public Optional<Etudiant> getEtudiantById(Long id) {
		return etudiantRepository.findById(id);
	}

	@Override
	public void addEtudiantToGroupe(Etudiant etudiant, Groupe groupe) {
		groupe.addEtudiant(etudiant);
		groupeRepository.save(groupe);
		addEtudiantToFiliere(etudiant, groupe.getFiliere());
	}

	@Override
	public Etudiant saveEtudiant(Etudiant etudiant) {
		return etudiantRepository.save(etudiant);
	}

	@Override
	@Transactional
	public EtudiantResponse saveEtudiant(CreateEtudiantRequest request) {
		User user = userService.createEtudiantUser(request.getCne(), request.getApogee());
		Etudiant etudiant = new Etudiant(request.getNom(), request.getPrenom(), "", user);
		addEtudiantToFiliere(etudiant, filiereRepository.findById(request.getFiliere()).get());
		addEtudiantToGroupe(etudiant, groupeRepository.findById(request.getGroupe()).get());
		etudiantRepository.save(etudiant);
		return ModelMapper.mapEtudiantToEtudiantResponse(etudiant);
	}

	@Override
	public Etudiant archiverEtudiant(Etudiant etudiant) {
		etudiant.setArchived(true);
		etudiantRepository.save(etudiant);
		return etudiant;
	}

	@Transactional
	@Override
	public void addEtudiantToFiliere(Etudiant etudiant, Filiere filiere) {
		etudiant.setFiliere(filiere);
		etudiantRepository.save(etudiant);
		groupeRepository.findByFiliereAndCategorie(filiere, GroupeCategorie.COURS).forEach(groupe -> {
			groupe.addEtudiant(etudiant);
			groupeRepository.save(groupe);
		});
	}

	@Transactional
	@Override
	public void ajouterListEtudiantsExcel(String filiereOrGroupe, Long id, MultipartFile excelDataFile)
			throws ExcelFileCellNotKnown, EtudiantsExcelFileHasWrogFormat {
		try {
			Workbook workbook = new XSSFWorkbook(excelDataFile.getInputStream());
			Sheet datatypeSheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = datatypeSheet.iterator();
			while (rowIterator.hasNext()) {
				Row currentRow = rowIterator.next();
				if (currentRow.getRowNum() == 0) {
					if(!isFileHasRightFormat(currentRow)) throw new EtudiantsExcelFileHasWrogFormat();
					continue;
				}
				Iterator<Cell> cellIterator = currentRow.iterator();
				User user = new User();
				Etudiant etudiant = new Etudiant();
				boolean saveCurrentEtudiant = true; // If an etudiant is duplicated, set to false and end loop to skip
				while (cellIterator.hasNext()) {
					Cell currentCell = cellIterator.next();
					switch (currentCell.getColumnIndex()){
						case 0:
							String cne = String.valueOf((long)currentCell.getNumericCellValue());
							if(userRepository.existsByUsername(cne)){
								saveCurrentEtudiant = false;
								break;
							}
							user.setUsername(cne);
							break;
						case 1:
							String apogee = String.valueOf((long)currentCell.getNumericCellValue());
							user.setPassword(new BCryptPasswordEncoder().encode(apogee));
							break;
						case 2:
							etudiant.setNom(currentCell.getStringCellValue());
							break;
						case 3:
							etudiant.setPrenom(currentCell.getStringCellValue());
							break;
						default:
							throw new ExcelFileCellNotKnown();
					}

				}
				if(!saveCurrentEtudiant)
					continue; // Break from current row
				user.getRoles().add(roleRepository.findByName(RoleName.ROLE_ETUDIANT));
				etudiant.setUser(user);
				etudiantRepository.save(etudiant);
				if(filiereOrGroupe.equalsIgnoreCase("filiere"))
					addEtudiantToFiliere(etudiant, filiereRepository.findById(id).get());
				else
					addEtudiantToGroupe(etudiant, groupeRepository.findById(id).get());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    @Override
    public List<AbsenceResponse> getAbsences(Long id) {
        return absenceRepository.findByEtudiant_Id(id).stream().map(ModelMapper::mapAbsenceToAbsenceResponse)
				.collect(Collectors.toList());
    }

    private boolean isFileHasRightFormat(Row currentRow) {
        Iterator<Cell> cellIterator = currentRow.iterator();
        while (cellIterator.hasNext()) {
            Cell currentCell = cellIterator.next();
            switch (currentCell.getColumnIndex()) {
                case 0:
                    if (!currentCell.getStringCellValue().equalsIgnoreCase("CNE"))
                        return false;
                    break;
				case 1:
					if(!currentCell.getStringCellValue().equalsIgnoreCase("APOGEE"))
						return false;
					break;
				case 2:
					if(!currentCell.getStringCellValue().equalsIgnoreCase("NOM"))
						return false;
					break;
				case 3:
					if(!currentCell.getStringCellValue().equalsIgnoreCase("PRENOM"))
						return false;
					break;
				default:
					return false;
			}
		}
		return true;
	}


}
