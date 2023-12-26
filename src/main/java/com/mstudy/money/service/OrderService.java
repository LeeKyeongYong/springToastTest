package com.mstudy.money.service;

import org.springframework.stereotype.Service;

@Service
public class OrderService {
    public void checkAmmout(String orderId,String amount){
        //해당 주문번호의 최정 결제금액이 정말 amount와 일치하는지 확인하는 로직

        boolean matched=true;

        //만약에 일치하지 않는다면, 예외를 발생시킨다.
        if(!matched) throw new RuntimeException("결제금액이 일치하지 않습니다.");

    }
    public void setPaymentComplate(String orderId){

    }
}
