package project.domain.party.service;


import org.springframework.web.multipart.MultipartFile;
import project.domain.member.entity.MemberRole;
import project.domain.party.dto.request.EnterPartyDto;
import project.domain.party.dto.request.CreatePartyDto;
import project.domain.party.dto.request.PartyPasswordDto;
import project.domain.party.dto.response.PartyEnterDto;
import project.domain.party.dto.response.PartyLinkDto;

public interface PartyService {

    PartyLinkDto createParty(CreatePartyDto createPartyDto, MultipartFile photo);

    PartyLinkDto createLink(Long partyId);

    void enterParty(String partyLink);

    void checkPassword(EnterPartyDto enterPartyDto);

    PartyEnterDto memberPartyEnter(EnterPartyDto enterPartyDto);

    PartyEnterDto guestPartyEnter(EnterPartyDto enterPartyDto);

    void changePassword(Long partyId, PartyPasswordDto partyPasswordDto);

    void changePartyName(Long partyId, String partyName);

    void grantPartyUser(Long partyId, Long opponentId, MemberRole memberRole);
}
