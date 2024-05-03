package project.domain.directory.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.domain.directory.collection.DataType;
import project.domain.directory.collection.Directory;
import project.domain.directory.collection.File;
import project.domain.directory.collection.Trash;
import project.domain.directory.collection.TrashBin;
import project.domain.directory.dto.request.CreateDirDto;
import project.domain.directory.dto.request.MoveDirDto;
import project.domain.directory.dto.response.DirDto;
import project.domain.directory.dto.response.GetDirDto;
import project.domain.directory.dto.response.RenameDirDto;
import project.domain.directory.mapper.DirMapper;
import project.domain.directory.mapper.GetDirMapper;
import project.domain.directory.mapper.RenameDirMapper;
import project.domain.directory.repository.DirectoryRepository;
import project.domain.directory.repository.FileRepository;
import project.domain.directory.repository.TrashBinRepository;
import project.domain.directory.repository.TrashRepository;
import project.domain.party.entity.Party;
import project.domain.party.repository.PartyRepository;
import project.domain.photo.entity.Photo;
import project.global.exception.BusinessLogicException;
import project.global.exception.ErrorCode;

@Service
@AllArgsConstructor
@Slf4j
public class DirectoryServiceImpl implements DirectoryService {


    private final FileRepository fileRepository;
    private final PartyRepository partyRepository;
    private final DirectoryRepository directoryRepository;
    private final TrashRepository trashRepository;
    private final TrashBinRepository trashBinRepository;

    private final DirMapper dirMapper;
    private final RenameDirMapper renameDirMapper;
    private final GetDirMapper getDirMapper;

    @Override
    public GetDirDto getDir(String dirId) {
        Directory dir = findById(dirId);
        return getDirMapper.toGetDirDto(
            dir,
            getParentDirName(dir),
            getChildNameList(dir),
            getPhotoUrlList(dir)
            );
    }

    @Override
    @Transactional
    public GetDirDto createDir(CreateDirDto request) {
        // 새로운 dir 생성
        Directory childDir = Directory.builder()
            .id(generateId())
            .dirName(request.getDirName())
            .parentDirId(request.getParentDirId())
            .build();
        // parentDir에 새로운 dir 추가
        Directory parentDir = findById(request.getParentDirId());
        parentDir.getChildDirIdList().add(childDir.getId());

        directoryRepository.saveAll(toList(childDir, parentDir));

        return getDirMapper.toGetDirDto(
            parentDir,
            getParentDirName(parentDir),
            getChildNameList(parentDir),
            getPhotoUrlList(parentDir)
        );
    }

    @Override
    @Transactional
    public GetDirDto moveDir(MoveDirDto request) {
        String dirId = request.getDirId();
        String toDirId = request.getToDirId();

        Directory dir = findById(dirId);
        Directory fromDir = findById(dir.getParentDirId());
        Directory toDir = findById(toDirId);
        // fromDirId : 자식 리스트에서 dirId 제거
        fromDir.getChildDirIdList().remove(dirId);
        // toDirId : 자식 리스트에 dirId 추가
        toDir.getChildDirIdList().add(dirId);
        // dirId : 부로를 toDirId로 수정
        dir.setParentDirId(toDirId);

        directoryRepository.saveAll(toList(dir, fromDir, toDir));
        return getDirMapper.toGetDirDto(
            fromDir,
            getParentDirName(fromDir),
            getChildNameList(fromDir),
            getPhotoUrlList(fromDir)
        );
    }

    @Override
    @Transactional
    public GetDirDto deleteDir(String dirId) {
        // 부모의 child list에서 해당 dirId 제거
        Directory dir = findById(dirId);
        Directory parentDir = findById(dir.getParentDirId());
        parentDir.getChildDirIdList().remove(dirId);
        directoryRepository.save(parentDir);
        // child 는 그대로 휴지통으로 임시 저장
        saveDirtoTrash(dir);
        saveDirInTrashBin(dir);
        // child 삭제
        directoryRepository.deleteById(dirId);

        return getDirMapper.toGetDirDto(
            parentDir,
            getParentDirName(parentDir),
            getChildNameList(parentDir),
            getPhotoUrlList(parentDir)
        );
    }

    @Override
    @Transactional
    public RenameDirDto renameDir(project.domain.directory.dto.request.RenameDirDto request) {
        String dirId = request.getDirId();
        String newName = request.getNewName();
        Directory dir = findById(dirId);
        dir.setDirName(newName);
        directoryRepository.save(dir);
        return renameDirMapper.toRenameDirDto(dir);
    }

    @Override
    public Directory findById(String dirId) {
        return directoryRepository.findById(dirId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.DIRECTORY_NOE_FOUND));
    }

    @Override
    public String generateId() {
        StringBuilder sb = new StringBuilder();
        sb.append(UUID.randomUUID()).append(LocalDateTime.now());
        return String.valueOf(sb);
    }

    @Override
    @Transactional
    public DirDto initDirPartyTest(Long partyId) {
        Party findParty = partyRepository.findById(partyId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.PARTY_NOT_FOUND));
        // 만약 party에 이미 rootDir이 있다면 나가리
        if(findParty.getRootDirId() != null) {
            throw new BusinessLogicException(ErrorCode.PARTY_ALREADY_HAVE_ROOT_DIR);
        }

        // 기본 폴더 생성 및 저장
        Directory rootDir = directoryRepository.save(
            Directory.builder()
                .id(generateId())
                .dirName("root")
                .parentDirId("")
                .build());
        // party에 rootDirId 저장
        findParty.setRootDirId(rootDir.getId());
        return dirMapper.toDirDto(rootDir);
    }

    @Override
    @Transactional
    public void initDirParty(Party party) {
        // 만약 party에 이미 rootDir이 있다면 나가리
        if(party.getRootDirId() != null) {
            throw new BusinessLogicException(ErrorCode.PARTY_ALREADY_HAVE_ROOT_DIR);
        }

        // 기본 폴더 생성 및 저장
        Directory rootDir = directoryRepository.save(
            Directory.builder()
                .id(generateId())
                .dirName("root")
                .parentDirId("")
                .build());
        // party에 rootDirId 저장
        party.setRootDirId(rootDir.getId());
    }

    @Override
    public List<Directory> toList(Directory... directories) {
        return new ArrayList<>(Arrays.asList(directories));
    }

    @Override
    public List<String> getChildNameList(Directory dir) {
        List<String> childNameList = new ArrayList<>();
        List<String> childIdList = dir.getChildDirIdList();
        for (String childId : childIdList) {
            // child 가져와서 이름 채우기
            Directory childDir = findById(childId);
            childNameList.add(childDir.getDirName());
        }

        return childNameList;
    }

    @Override
    public String getParentDirName(Directory directory) {
        if (Objects.equals(directory.getParentDirId(), "")) {
            return "";  // 또는 ""로 반환
        }
        log.info("여기");
        Directory parentDir = findById(directory.getParentDirId());
        return parentDir != null ? parentDir.getDirName() : "No Parent";
    }

    @Override
    public List<String> getChildDirNameList(Directory directory) {
        List<String> childDirNameList = new ArrayList<>();
        List<Directory> childDirList = directoryRepository.findAllById(directory.getChildDirIdList());
        for (Directory childDir : childDirList) {
            childDirNameList.add(childDir.getDirName());
        }

        return childDirNameList;
    }

    @Override
    public List<String> getPhotoUrlList(Directory directory) {
        List<String> photoUrlList = new ArrayList<>();
        List<File> FileList =  fileRepository.findAllById(directory.getFileIdList());
        ModelMapper modelMapper = new ModelMapper();
        for (File file : FileList) {
            Photo photo = modelMapper.map(file.getPhoto(), Photo.class);
            photoUrlList.add(photo.getPhotoUrl().getPhotoUrl());
        }
        return photoUrlList;
    }

    @Override
    public String getRootDirId(Directory dir) {
        int cnt = 0;
        while(!dir.getParentDirId().equals("")) {
            dir = findById(dir.getParentDirId());
            cnt++;
            if(cnt > 100){
                throw new BusinessLogicException(ErrorCode.ROOTDIR_NOT_FOUND);
            }
        }
        return dir.getId();
    }

    public Trash saveDirtoTrash(Directory dir) {
        return trashRepository.save(Trash.builder()
            .id(generateId())
            .dataType(DataType.DIRECTORY)
            .content(dir)
            .deadLine(LocalDate.now().plusWeeks(2))
            .build());
    }


    public void saveDirInTrashBin(Directory dir) {
        // parentDirId = ""이면 while 탈출
        String rootDirId = getRootDirId(dir);
        Party party = partyRepository.findPartyByRootDirId(rootDirId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.PARTY_NOT_FOUND));
        Long partyId = party.getId();
        TrashBin trashBin = trashBinRepository.findById(partyId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.TRASHBIN_NOT_FOUND));
        trashBin.getDirIdList().add(dir.getId());
        trashBinRepository.save(trashBin);
    }

}
