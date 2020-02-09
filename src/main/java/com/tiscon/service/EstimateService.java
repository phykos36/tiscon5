package com.tiscon.service;

import com.tiscon.code.OptionalServiceType;
import com.tiscon.code.PackageType;
import com.tiscon.dao.EstimateDao;
import com.tiscon.domain.Customer;
import com.tiscon.domain.CustomerOptionService;
import com.tiscon.domain.CustomerPackage;
import com.tiscon.dto.UserOrderDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.tiscon.jackson.Json2Java;

import java.net.HttpURLConnection;
import java.net.URL;
/**
 * 引越し見積もり機能において業務処理を担当するクラス。
 *
 * @author Oikawa Yumi
 */
@Service
public class EstimateService {

    /** 引越しする距離の1 kmあたりの料金[円] */
    private static final int PRICE_PER_DISTANCE = 100;

    private final EstimateDao estimateDAO;

    /**
     * コンストラクタ
     *
     * @param estimateDAO EstimateDaoクラス
     */
    public EstimateService(EstimateDao estimateDAO) {
        this.estimateDAO = estimateDAO;
    }

    /**
     * 見積もり依頼をDBに登録する。
     *
     * @param dto 見積もり依頼情報
     */
    @Transactional
    public void registerOrder(UserOrderDto dto) {
        Customer customer = new Customer();
        BeanUtils.copyProperties(dto, customer);
        estimateDAO.insertCustomer(customer);

        if (dto.getWashingMachineInstallation()) {
            CustomerOptionService washingMachine = new CustomerOptionService();
            washingMachine.setCustomerId(customer.getCustomerId());
            washingMachine.setServiceId(OptionalServiceType.WASHING_MACHINE.getCode());
            estimateDAO.insertCustomersOptionService(washingMachine);
        }

        List<CustomerPackage> packageList = new ArrayList<>();

        packageList.add(new CustomerPackage(customer.getCustomerId(), PackageType.BOX.getCode(), dto.getBox()));
        packageList.add(new CustomerPackage(customer.getCustomerId(), PackageType.BED.getCode(), dto.getBed()));
        packageList.add(new CustomerPackage(customer.getCustomerId(), PackageType.BICYCLE.getCode(), dto.getBicycle()));
        packageList.add(new CustomerPackage(customer.getCustomerId(), PackageType.WASHING_MACHINE.getCode(), dto.getWashingMachine()));
        estimateDAO.batchInsertCustomerPackage(packageList);
    }

    /**
     * 見積もり依頼に応じた概算見積もりを行う。
     *
     * @param dto 見積もり依頼情報
     * @return 概算見積もり結果の料金
     */
    public Integer getPrice(UserOrderDto dto) {
//        double distance = estimateDAO.getDistance(dto.getOldPrefectureId(), dto.getNewPrefectureId());
//        // 小数点以下を切り捨てる
//        int distanceInt = (int) Math.floor(distance);

        int distanceInt = getDirectionDistance(dto.getOldAddress(), dto.getNewAddress());

        // 距離当たりの料金を算出する
        int priceForDistance = distanceInt * PRICE_PER_DISTANCE;

        int boxes = getBoxForPackage(dto.getBox(), PackageType.BOX)
                + getBoxForPackage(dto.getBed(), PackageType.BED)
                + getBoxForPackage(dto.getBicycle(), PackageType.BICYCLE)
                + getBoxForPackage(dto.getWashingMachine(), PackageType.WASHING_MACHINE);

        // 箱に応じてトラックの種類が変わり、それに応じて料金が変わるためトラック料金を算出する。
        int pricePerTruck = estimateDAO.getPricePerTruck(boxes);

        // オプションサービスの料金を算出する。
        int priceForOptionalService = 0;

        if (dto.getWashingMachineInstallation()) {
            priceForOptionalService = estimateDAO.getPricePerOptionalService(OptionalServiceType.WASHING_MACHINE.getCode());
        }

        return priceForDistance + pricePerTruck + priceForOptionalService;
    }

    /**
     * 荷物当たりの段ボール数を算出する。
     *
     * @param packageNum 荷物数
     * @param type       荷物の種類
     * @return 段ボール数
     */
    private int getBoxForPackage(int packageNum, PackageType type) {
        return packageNum * estimateDAO.getBoxPerPackage(type.getCode());
    }

    /**
     * 住所間の経路の距離を取得する。
     *
     * @param addressFrom 引っ越し元の都道府県
     * @param addressTo   引越し先の都道府県
     * @return 距離[km]
     */

    public static int getDirectionDistance(String addressFrom, String addressTo) {
        HttpURLConnection http = null;
        StringBuffer json = new StringBuffer();
        String apiUrl = "https://maps.googleapis.com/maps/api/directions/json";
        apiUrl += "?origin="+addressFrom;
        apiUrl += "&destination="+addressTo;
        apiUrl += "&key=AIzaSyCXTD37wn_woDNvf1nWo4oTf4q8xsTvIqI";
        try {
            URL url = new URL(apiUrl);
            http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null)
                json.append(line);

        } catch (IOException e) {
            System.out.println(e);
        } finally {
            http.disconnect();
        }

        Json2Java json2Java = new Json2Java();
        int distanceMeter = json2Java.main(json.toString());
        int distanceKm = (int) Math.floor(distanceMeter / 1000);

        return distanceKm;
    }
}