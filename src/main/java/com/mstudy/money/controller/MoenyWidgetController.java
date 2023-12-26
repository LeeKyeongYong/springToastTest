package com.mstudy.money.controller;

import com.mstudy.money.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


@Controller
public class MoenyWidgetController {

    @Value("${api.key}")
    private String API_KEY;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OrderService orderService;

    @RequestMapping(value="/confirm")
    public ResponseEntity<JSONObject> confirmPayment(@RequestBody String jsonBody) throws Exception{
        JSONParser parser = new JSONParser();
        String OrderId;
        String amount;
        String paymentKey;

        try{//클라이언트에서 요청받은 JSON바디

            JSONObject requestData=(JSONObject) parser.parse(jsonBody);
            paymentKey = (String)requestData.get("paymentKey");
            OrderId=(String)requestData.get("orderId");
            amount=(String)requestData.get("amount");

        }catch(ParseException e){
            throw new RuntimeException(e);
        }

        //체크
        orderService.checkAmmout(OrderId,amount);

        JSONObject obj = new JSONObject();
        obj.put("orderId",OrderId);
        obj.put("amount",amount);
        obj.put("paymentKey",paymentKey);

        // TODO: 개발자센터에 로그인해서 내 결제위젯 연동 키 > 시크릿 키를 입력하기.
        // 시크릿 키는 외부에 연동하면 안된다.
        // @Docs https://docs.tosspayments.com/reference/using-api/api-keys

        String apiKey = API_KEY;

        // 토스페이먼츠 api는 시크릿 키를 사용자 ID로 사용하고, 비밀번호는 사용하지 않는다.
        // 비밀번호가 없다는 것을 알리기 위해 시크릿 키 뒤에 콜론을 추가한다.
        // @docs https://docs.tosspayments.com/reference/using-api/authorization#%EC%9D%B8%EC%A6%9D
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodeBytes =encoder.encode((apiKey+":").getBytes("UTF-8"));
        String authorizations = "Basic "+new String(encodeBytes,0,encodeBytes.length);

        //결제 승인 API 호출하기
        //결제를 승인하면 결제수단에서 금액이 차감된다.
        // @docs https://docs.tosspayments.com/guides/payment-widget/integration#3-결제-승인하기
        URL url=new URL("https://api.tosspayments.com/v1/payments/confirm");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization",authorizations);
        connection.setRequestProperty("Content-Type","applicaiton/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(obj.toString().getBytes("UTF-8"));

        int code =connection.getResponseCode();
        boolean isSuccess = code == 200 ? true: false;
        //결제승인이 완료
        if(isSuccess){
            orderService.setPaymentComplate(OrderId);
        } else {
            throw new RuntimeException("결제 승인 실패");
        }

        InputStream responseStream = isSuccess ? connection.getInputStream():connection.getErrorStream();

        // TODO: 결제 성공 및 실패 비즈니스 로직을 구현하기
        Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8);
        JSONObject jsonObject = (JSONObject) parser.parse(reader);
        responseStream.close();
        return ResponseEntity.status(code).body(jsonObject);
    }

    /**
     * 인증성공처리
     *
     * @param request
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/success",method= RequestMethod.GET)
    public String paymentRequest(HttpServletRequest request, Model model)throws Exception{
        return "/success";
    }

    @RequestMapping(value="/",method= RequestMethod.GET)
    public String index(HttpServletRequest request,Model model)throws Exception{
        return"checkout";
    }

    /**
     * 인증 실패 처리
     *
     * @param request
     * @param model
     * @return
     * @throws Exception
     */

    @RequestMapping(value="/fail",method = RequestMethod.GET)
    public String failPayment(HttpServletRequest request,Model model){
        String failCode = request.getParameter("code");
        String failMessage = request.getParameter("message");

        model.addAttribute("code",failCode);
        model.addAttribute("message",failMessage);

        return "/fail";
    }

}
