package dataservice;

import enums.SubscriptionState;
import enums.WayOfCall;
import enums.WayOfUnsubscribe;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Created by zlk on 2018/10/22
 */
public interface OperationDataService {

    /**
     * 连接数据库
     */
//    public void ConnectDatabase();

    /**
     * 根据电话号码找用户正在进行中的套餐
     * @param phoneNumber
     * @param subscriptionState
     */
    public void getSubscriptionInProcess(String phoneNumber, SubscriptionState subscriptionState);

    /**
     * 根据电话号码和套餐id订阅套餐
     * @param phoneNumber
     * @param p_id
     */
    public void order(String phoneNumber,String p_id);

    /**
     * 退订（立即生效和次月生效）
     * @param phoneNumber
     * @param p_id
     * @param wayOfUnsubscribe
     */
    public void unsubscribe(String phoneNumber, String p_id, WayOfUnsubscribe wayOfUnsubscribe);

    /**
     * 通话
     * @param phoneNumber
     * @param callTime
     */
    public void call(String phoneNumber, int callTime, WayOfCall wayOfCall);

    /**
     * 在本地使用流量
     * @param phoneNumber
     * @param traffic
     */
    public void useTrafficInTheLocalArea(String phoneNumber, float traffic);

    /**
     * 在外地使用流量
     * @param phoneNumber
     * @param traffic
     */
    public void useTrafficInTheField(String phoneNumber, float traffic);

    /**
     * 发送一条短信
     * @param phoneNumber
     */
    public void message(String phoneNumber);

    /**
     * 生成账单
     * @param phoneNumber
     */
    public void createBill(String phoneNumber);

    /**
     * 显示所有的套餐
     */
    public void showAllPackage();

    /**
     * 准确查询
     * @param sql
     * @return
     */
    public ResultSet executeQuery(String sql);

    /**
     * 执行sql语句
     * @param sql
     * @return
     */
    public boolean execute(String sql);

}
