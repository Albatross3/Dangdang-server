package com.dangdang.server.domain.pay.daangnpay.domain.payMember.application;

import static com.dangdang.server.domain.pay.daangnpay.global.vo.TrustAccount.OPEN_BANKING_CONTRACT_ACCOUNT;
import static com.dangdang.server.global.exception.ExceptionCode.PAY_MEMBER_NOT_FOUND;

import com.dangdang.server.domain.pay.daangnpay.domain.connectionAccount.exception.EmptyResultException;
import com.dangdang.server.domain.pay.daangnpay.domain.payMember.domain.PayMemberRepository;
import com.dangdang.server.domain.pay.daangnpay.domain.payMember.domain.entity.PayMember;
import com.dangdang.server.domain.pay.daangnpay.domain.payMember.dto.PayRequest;
import com.dangdang.server.domain.pay.daangnpay.domain.payMember.dto.PayResponse;
import com.dangdang.server.domain.pay.daangnpay.domain.payUsageHistory.application.PayUsageHistoryService;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.application.OpenBankingFacadeService;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.dto.OpenBankingDepositRequest;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.dto.OpenBankingResponse;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.dto.OpenBankingWithdrawRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class PayMemberService {

  private final OpenBankingFacadeService openBankingFacadeService;
  private final PayUsageHistoryService payUsageHistoryService;
  private final PayMemberRepository payMemberRepository;

  public PayMemberService(OpenBankingFacadeService openBankingFacadeService,
      PayUsageHistoryService payUsageHistoryService, PayMemberRepository payMemberRepository
  ) {
    this.openBankingFacadeService = openBankingFacadeService;
    this.payUsageHistoryService = payUsageHistoryService;
    this.payMemberRepository = payMemberRepository;
  }

  // TODO : 비밀번호 입력 로직 추가

  private PayMember getPayMember(Long memberId) {
    return payMemberRepository.findByMemberId(memberId)
        .orElseThrow(() -> new EmptyResultException(PAY_MEMBER_NOT_FOUND));
  }

  private OpenBankingWithdrawRequest createOpenBankingWithdrawRequest(Long payMemberId,
      PayRequest payRequest) {
    return new OpenBankingWithdrawRequest(payMemberId, OPEN_BANKING_CONTRACT_ACCOUNT.getAccountId(),
        payRequest.bankAccountId(), payRequest.amount());
  }

  private OpenBankingDepositRequest createOpenBankingDepositRequest(Long payMemberId,
      PayRequest payRequest) {
    return new OpenBankingDepositRequest(payMemberId, payRequest.bankAccountId(),
        OPEN_BANKING_CONTRACT_ACCOUNT.getAccountId(), payRequest.amount());
  }

  /**
   * 당근머니 충전
   */
  @Transactional
  public PayResponse charge(Long memberId, PayRequest payRequest) {
    PayMember payMember = getPayMember(memberId);
    OpenBankingWithdrawRequest openBankingWithdrawRequest = createOpenBankingWithdrawRequest(
        payMember.getId(), payRequest);
    OpenBankingResponse openBankingResponse = openBankingFacadeService.withdraw(
        openBankingWithdrawRequest);

    int balanceMoney = payMemberAddMoney(payMember, payRequest);

    payUsageHistoryService.addUsageHistory(openBankingResponse, balanceMoney, payMember);

    return PayResponse.from(openBankingResponse, balanceMoney);
  }

  private int payMemberAddMoney(PayMember payMember, PayRequest payRequest) {
    int amount = payRequest.amount();
    return payMember.addMoney(amount);
  }
}
