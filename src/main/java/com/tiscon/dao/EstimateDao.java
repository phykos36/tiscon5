package com.tiscon.dao;

import com.tiscon.domain.*;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 引越し見積もり機能においてDBとのやり取りを行うクラス。
 *
 * @author Oikawa Yumi
 */
@Component
public class EstimateDao {

    /** データベース・アクセスAPIである「JDBC」を使い、名前付きパラメータを用いてSQLを実行するクラス */
    private final NamedParameterJdbcTemplate parameterJdbcTemplate;

    /**
     * コンストラクタ
     *
     * @param parameterJdbcTemplate NamedParameterJdbcTemplateクラス
     */
    public EstimateDao(NamedParameterJdbcTemplate parameterJdbcTemplate) {
        this.parameterJdbcTemplate = parameterJdbcTemplate;
    }

    /**
     * 顧客テーブルに登録する。
     *
     * @param customer 顧客情報
     * @return 登録件数
     */
    public int insertCustomer(Customer customer) {
        String sql = "UPDATE CUSTOMER SET CUSTOMER_NAME = :customerName, TEL = :tel, EMAIL = :email, OLD_ADDRESS = :oldAddress, NEW_ADDRESS = :newAddress"
        +" WHERE CUSTOMER_NAME = :customerName AND TEL = :tel";
        //  keyHolder = new GeneratedKeyHolder();
        int resultNum = parameterJdbcTemplate.update(sql, new BeanPropertySqlParameterSource(customer));
        if(resultNum == 0){
            parameterJdbcTemplate.update("INSERT INTO CUSTOMER(CUSTOMER_NAME, TEL, EMAIL, OLD_ADDRESS, NEW_ADDRESS)"
                            +" VALUES(:customerName, :tel, :email, :oldAddress, :newAddress)",
                    new BeanPropertySqlParameterSource(customer));
        }
        // customer.setCustomerId(keyHolder.getKey().intValue());
        customer.setCustomerId(parameterJdbcTemplate.queryForObject("SELECT CUSTOMER_ID FROM CUSTOMER"
                        +" WHERE CUSTOMER_NAME = :customerName AND TEL = :tel",
                new BeanPropertySqlParameterSource(customer),Integer.TYPE));
        return resultNum;
    }

    /**
     * オプションサービス_顧客テーブルに登録する。
     *
     * @param optionService オプションサービス_顧客に登録する内容
     * @return 登録件数
     */
    public int insertCustomersOptionService(CustomerOptionService optionService) {
        String sql = "INSERT INTO CUSTOMER_OPTION_SERVICE(CUSTOMER_ID, SERVICE_ID)"
                + " VALUES(:customerId, :serviceId)";
        return parameterJdbcTemplate.update(sql, new BeanPropertySqlParameterSource(optionService));
    }

    /**
     * 顧客_荷物テーブルに登録する。
     *
     * @param packages 登録する荷物
     * @return 登録件数
     */
    public int[] batchInsertCustomerPackage(List<CustomerPackage> packages) {

        String sql = "UPDATE CUSTOMER_PACKAGE SET CUSTOMER_ID = :customerId, PACKAGE_ID = :packageId, PACKAGE_NUMBER = :packageNumber"
                +" WHERE CUSTOMER_ID = :customerId AND PACKAGE_ID = :packageId";
        SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(packages.toArray());
        int[] resultNums = parameterJdbcTemplate.batchUpdate(sql, batch);
        if(resultNums[0]==0 || resultNums[1]==0 || resultNums[2]==0 || resultNums[3]==0){
            sql = "INSERT INTO CUSTOMER_PACKAGE(CUSTOMER_ID, PACKAGE_ID, PACKAGE_NUMBER)"
                    + " VALUES(:customerId, :packageId, :packageNumber)";
            resultNums = parameterJdbcTemplate.batchUpdate(sql, batch);
        }
        return resultNums;
    }

    /**
     * 都道府県テーブルに登録されているすべての都道府県を取得する。
     *
     * @return すべての都道府県
     */
    public List<Prefecture> getAllPrefectures() {
        String sql = "SELECT PREFECTURE_ID, PREFECTURE_NAME FROM PREFECTURE";
        return parameterJdbcTemplate.query(sql,
                BeanPropertyRowMapper.newInstance(Prefecture.class));
    }

    /**
     * 都道府県間の距離を取得する。
     *
     * @param prefectureIdFrom 引っ越し元の都道府県
     * @param prefectureIdTo   引越し先の都道府県
     * @return 距離[km]
     */
    public double getDistance(String prefectureIdFrom, String prefectureIdTo) {
        // 都道府県のFromとToが逆転しても同じ距離となるため、「そのままの状態のデータ」と「FromとToを逆転させたデータ」をくっつけた状態で距離を取得する。
        String sql = "SELECT DISTANCE FROM (" +
                "SELECT PREFECTURE_ID_FROM, PREFECTURE_ID_TO, DISTANCE FROM PREFECTURE_DISTANCE UNION ALL " +
                "SELECT PREFECTURE_ID_TO PREFECTURE_ID_FROM ,PREFECTURE_ID_FROM PREFECTURE_ID_TO ,DISTANCE FROM PREFECTURE_DISTANCE) " +
                "WHERE PREFECTURE_ID_FROM  = :prefectureIdFrom AND PREFECTURE_ID_TO  = :prefectureIdTo";

        PrefectureDistance prefectureDistance = new PrefectureDistance();
        prefectureDistance.setPrefectureIdFrom(prefectureIdFrom);
        prefectureDistance.setPrefectureIdTo(prefectureIdTo);

        double distance;
        try {
            distance = parameterJdbcTemplate.queryForObject(sql, new BeanPropertySqlParameterSource(prefectureDistance), double.class);
        } catch (IncorrectResultSizeDataAccessException e) {
            distance = 0;
        }
        return distance;
    }

    /**
     * 荷物ごとの段ボール数を取得する。
     *
     * @param packageId 荷物ID
     * @return 段ボール数
     */
    public int getBoxPerPackage(int packageId) {
        String sql = "SELECT BOX FROM PACKAGE_BOX WHERE PACKAGE_ID = :packageId";

        SqlParameterSource paramSource = new MapSqlParameterSource("packageId", packageId);
        return parameterJdbcTemplate.queryForObject(sql, paramSource, Integer.class);
    }

    /**
     * 段ボール数に応じたトラック料金を取得する。
     *
     * @param boxNum 総段ボール数
     * @return 料金[円]
     */
    public int getPricePerTruck(int boxNum) {
        String sql = "SELECT PRICE FROM TRUCK_CAPACITY WHERE MAX_BOX >= :boxNum ORDER BY PRICE LIMIT 1";

        SqlParameterSource paramSource = new MapSqlParameterSource("boxNum", boxNum);
        int truckPrice=0;
        int maxBox;
        int maxPrice;
        try {
            truckPrice = parameterJdbcTemplate.queryForObject(sql, paramSource, Integer.class);
        } catch(IncorrectResultSizeDataAccessException e){
            maxBox = parameterJdbcTemplate.queryForObject("SELECT MAX_BOX FROM TRUCK_CAPACITY ORDER BY PRICE DESC LIMIT 1", (SqlParameterSource) null, Integer.class);
            maxPrice = parameterJdbcTemplate.queryForObject("SELECT PRICE FROM TRUCK_CAPACITY ORDER BY PRICE DESC LIMIT 1", (SqlParameterSource) null, Integer.class);
            truckPrice = maxPrice + getPricePerTruck(boxNum - maxBox);
        }
        return truckPrice;
    }

    /**
     * オプションサービスの料金を取得する。
     *
     * @param serviceId サービスID
     * @return 料金
     */
    public int getPricePerOptionalService(int serviceId) {
        String sql = "SELECT PRICE FROM OPTIONAL_SERVICE WHERE SERVICE_ID = :serviceId";

        SqlParameterSource paramSource = new MapSqlParameterSource("serviceId", serviceId);
        return parameterJdbcTemplate.queryForObject(sql, paramSource, Integer.class);
    }
}
