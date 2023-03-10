package com.dangdang.server.domain.memberTown.application;

import com.dangdang.server.domain.common.StatusType;
import com.dangdang.server.domain.member.domain.MemberRepository;
import com.dangdang.server.domain.member.domain.entity.Member;
import com.dangdang.server.domain.member.exception.MemberNotFoundException;
import com.dangdang.server.domain.memberTown.domain.MemberTownRepository;
import com.dangdang.server.domain.memberTown.domain.entity.MemberTown;
import com.dangdang.server.domain.memberTown.domain.entity.RangeType;
import com.dangdang.server.domain.memberTown.domain.entity.TownAuthStatus;
import com.dangdang.server.domain.memberTown.dto.request.MemberTownCertifyRequest;
import com.dangdang.server.domain.memberTown.dto.request.MemberTownRangeRequest;
import com.dangdang.server.domain.memberTown.dto.request.MemberTownRequest;
import com.dangdang.server.domain.memberTown.dto.response.MemberTownCertifyResponse;
import com.dangdang.server.domain.memberTown.dto.response.MemberTownRangeResponse;
import com.dangdang.server.domain.memberTown.dto.response.MemberTownResponse;
import com.dangdang.server.domain.memberTown.exception.MemberTownNotFoundException;
import com.dangdang.server.domain.memberTown.exception.NotAppropriateCountException;
import com.dangdang.server.domain.town.domain.TownRepository;
import com.dangdang.server.domain.town.domain.entity.Town;
import com.dangdang.server.domain.town.dto.AdjacentTownResponse;
import com.dangdang.server.domain.town.exception.TownNotFoundException;
import com.dangdang.server.global.exception.ExceptionCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberTownService {

  private final int MY_TOWN_CERTIFY_DISTANCE = 10;
  private final MemberTownRepository memberTownRepository;
  private final TownRepository townRepository;
  private final MemberRepository memberRepository;

  public MemberTownService(MemberTownRepository memberTownRepository,
      TownRepository townRepository, MemberRepository memberRepository) {
    this.memberTownRepository = memberTownRepository;
    this.townRepository = townRepository;
    this.memberRepository = memberRepository;
  }


  @Transactional
  public MemberTownResponse createMemberTown(MemberTownRequest memberTownRequest, Long memberId) {

    // ????????? memberTown ????????? 1??? ???????????? ?????? ??????
    List<MemberTown> memberTownList = memberTownRepository.findByMemberId(memberId);
    if (memberTownList.size() != 1) {
      throw new NotAppropriateCountException(ExceptionCode.NOT_APPROPRIATE_COUNT);
    }

    Town foundTown = getTownByTownName(memberTownRequest.townName());
    Member foundMember = getMemberByMemberId(memberId);

    MemberTown memberTown = new MemberTown(foundMember, foundTown);

    // ????????? ????????? Active, ????????? ?????? Inactive
    MemberTown existingMemberTown = memberTownList.get(0);
    existingMemberTown.updateMemberTownStatus(StatusType.INACTIVE);
    memberTownRepository.save(memberTown);

    return new MemberTownResponse(memberTownRequest.townName());
  }

  @Transactional
  public void deleteMemberTown(MemberTownRequest memberTownRequest, Long memberId) {
    // MemberTown ??? 2??? ???????????? ????????? ????????????
    List<MemberTown> memberTownList = memberTownRepository.findByMemberId(memberId);
    if (memberTownList.size() != 2) {
      throw new NotAppropriateCountException(ExceptionCode.NOT_APPROPRIATE_COUNT);
    }

    MemberTown memberTown1 = memberTownList.get(0);
    MemberTown memberTown2 = memberTownList.get(1);

    // Inactive ?????? ?????? -> Active ??? ?????? ?????? (??????)
    // Active ?????? ?????? -> Inactive ??? Active ??? ?????????  (?????? + ??????)
    if (memberTown1.getTownName().equals(memberTownRequest.townName())) {
      if (memberTown1.getStatus() == StatusType.ACTIVE) {
        memberTown2.updateMemberTownStatus(StatusType.ACTIVE);
      }
      memberTownRepository.delete(memberTown1);
    } else if (memberTown2.getTownName().equals(memberTownRequest.townName())) {
      if (memberTown2.getStatus() == StatusType.ACTIVE) {
        memberTown1.updateMemberTownStatus(StatusType.ACTIVE);
      }
      memberTownRepository.delete(memberTown2);
    } else {
      throw new MemberTownNotFoundException(ExceptionCode.MEMBER_TOWN_NOT_FOUND);
    }
  }

  @Transactional
  public MemberTownResponse changeActiveMemberTown(MemberTownRequest memberTownRequest, Long memberId) {
    // ???????????? Inactive, ?????? ???????????? Active
    // member ??? DB??? ????????? ??? id??? ????????? ????????? ????????? ??????
    // ??? ???????????? front ???????????? ?????? 2?????? ?????? ?????? ??? ????????? ?????? ???
    List<MemberTown> memberTownList = memberTownRepository.findByMemberId(memberId);
    if (memberTownList.size() != 2) {
      throw new NotAppropriateCountException(ExceptionCode.NOT_APPROPRIATE_COUNT);
    }
    for (MemberTown memberTown : memberTownList) {
      // ???????????? ????????? ?????? -> active
      if (memberTown.getTownName().equals(memberTownRequest.townName())) {
        memberTown.updateMemberTownStatus(StatusType.ACTIVE);
      } else {
        memberTown.updateMemberTownStatus(StatusType.INACTIVE);
      }
    }
    return new MemberTownResponse(memberTownRequest.townName());
  }

  @Transactional
  public MemberTownRangeResponse changeMemberTownRange(
      MemberTownRangeRequest memberTownRangeRequest, Long memberId) {
    Member foundMember = getMemberByMemberId(memberId);
    Town foundTown = getTownByTownName(memberTownRangeRequest.townName());

    MemberTown foundMemberTown = memberTownRepository
        .findByMemberIdAndTownId(foundMember.getId(), foundTown.getId())
        .orElseThrow(() -> new MemberTownNotFoundException(ExceptionCode.MEMBER_TOWN_NOT_FOUND));

    RangeType updatedRangeType = RangeType.getRangeType(memberTownRangeRequest.level());
    foundMemberTown.updateMemberTownRange(updatedRangeType);

    return new MemberTownRangeResponse(memberTownRangeRequest.townName(),
        memberTownRangeRequest.level());
  }

  @Transactional
  public MemberTownCertifyResponse certifyMemberTown(
      MemberTownCertifyRequest memberTownCertifyRequest, Long memberId) {
    boolean isCertified = false;
    // 1. ?????? ??????, ????????? ???????????? Town list ??????
    List<AdjacentTownResponse> towns = townRepository.findAdjacentTownsByPoint(
        memberTownCertifyRequest.latitude(),
        memberTownCertifyRequest.longitude(),
        MY_TOWN_CERTIFY_DISTANCE);
    // 2. Active ??? ????????? ????????? list ?????? ????????? ??????
    MemberTown activeMemberTown = memberTownRepository.findByMemberId(memberId)
        .stream()
        .filter(memberTown -> memberTown.getStatus() == StatusType.ACTIVE)
        .findFirst()
        .orElseThrow(() -> new MemberTownNotFoundException(ExceptionCode.MEMBER_TOWN_NOT_FOUND));

    for (AdjacentTownResponse adjacentTown : towns) {
      if (activeMemberTown.getTownName().equals(adjacentTown.getName())) {
        isCertified = true;
        activeMemberTown.updateMemberTownAuthStatus(TownAuthStatus.TOWN_CERTIFIED);
        break;
      }
    }
    return new MemberTownCertifyResponse(isCertified);
  }

  private Member getMemberByMemberId(Long memberId) {
    return memberRepository.findById(memberId)
        .orElseThrow(() -> new MemberNotFoundException(ExceptionCode.MEMBER_NOT_FOUND));
  }

  private Town getTownByTownName(String townName) {
    return townRepository.findByName(townName)
        .orElseThrow(() -> new TownNotFoundException(ExceptionCode.TOWN_NOT_FOUND));
  }


}
