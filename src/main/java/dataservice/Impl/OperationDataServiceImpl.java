package dataservice.Impl;

import dataservice.OperationDataService;
import enums.SubscriptionState;
import enums.WayOfCall;
import enums.WayOfUnsubscribe;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by zlk on 2018/10/22
 */
public class OperationDataServiceImpl implements OperationDataService {

    Connection connection = null;

    public OperationDataServiceImpl(Connection connection){
        this.connection = connection;
    }

    public void getSubscriptionInProcess(String phoneNumber, SubscriptionState subscriptionState){
        try {
            //通过电话号码找到用户id
            String u_id = getUserIdByPhoneNumber(phoneNumber);
            if(u_id.equals("")){
                System.out.println("该号码并未注册");
                return;
            }
            //通过用户id进行套餐查询
            ResultSet rs_2;
            if(subscriptionState==SubscriptionState.ON){
                rs_2 = executeQuery("select * from subscription where u_id="+u_id+" and s_state='正在进行中'");
            }else{
                rs_2 = executeQuery("select * from subscription where u_id="+u_id+" and s_state='已失效'");
            }
            while(rs_2.next()){
                String p_id = rs_2.getString("p_id");
//                System.out.println("套餐id： "+p_id);
                getConcreteSubscription(p_id);
                System.out.println("订阅时间： "+rs_2.getString("order_time"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void order(String phoneNumber,String p_id){
        try {
            String u_id = getUserIdByPhoneNumber(phoneNumber);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            execute("insert into subscription(u_id,p_id,order_time,s_state) value("+u_id+","+p_id+",'"+df.format(new Date())+"','即将生效')");
            System.out.println("订购成功！");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String phoneNumber, String p_id, WayOfUnsubscribe wayOfUnsubscribe){
        try {
            String u_id = getUserIdByPhoneNumber(phoneNumber);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            if(wayOfUnsubscribe==WayOfUnsubscribe.EFFECTIMMEDIATELY){ //立即生效
                //根据套餐使用比例退款
                List<Double> list = getPackageContent(p_id);
                double allValue = list.get(0);
                double functional_fee = list.get(1);
                double surplusValue = getSubscriptionContent(u_id,p_id);
                ResultSet resultSet = executeQuery("select * from user where u_id=" + u_id);
                float amount = 0;
                while (resultSet.next()) {
                    amount = Float.parseFloat(resultSet.getString("amount"));
                }
                execute("update user set amount=" + String.valueOf(amount + functional_fee*surplusValue/allValue) + " where u_id=" + u_id);
                System.out.println("已返还费用!");
                execute("update subscription set unsubscribe_time='"+df.format(new Date())+"', s_state='已失效',surplus_phoneminute=0,surplus_message=0,localTraffic=0,domesticTraffic=0 where u_id="+u_id+" and p_id="+p_id+" and s_state<>'已失效';");
                System.out.println("退订成功！");
            }else{  //次月生效
                String str = "";
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
                Calendar lastDate = Calendar.getInstance();
                lastDate.add(Calendar.MONTH,1);//减一个月
                lastDate.set(Calendar.DATE, 1);//把日期设置为当月第一天
                str=sdf.format(lastDate.getTime())+" 00:00:00";
                execute("update subscription set unsubscribe_time='"+str+"' where u_id="+u_id+" and p_id="+p_id+" and s_state<>'已失效';");
                System.out.println("退订成功！");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void call(String phoneNumber, int callTime, WayOfCall wayOfCall){
        try {
            int actual_callTime = callTime;
            String u_id = getUserIdByPhoneNumber(phoneNumber);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            if(wayOfCall==WayOfCall.DIAL) {
                ResultSet resultSet = executeQuery("select * from subscription where u_id=" + u_id + " and s_state='正在进行中'");
                float fee = 0;
                while (resultSet.next()) {
                    String str = "";
                    if (!(str = resultSet.getString("surplus_phoneminute")).equals("0")) {
                        int surplus_minute = Integer.parseInt(str);
                        if (callTime <= surplus_minute) {   //可使用套餐的拨打电话数
                            execute("update subscription set surplus_phoneminute=" + String.valueOf(surplus_minute - callTime) + " where s_id=" + resultSet.getString("s_id"));
                            callTime = 0;
                            continue;
                        } else {
                            execute("update subscription set surplus_phoneminute=0 where s_id=" + resultSet.getString("s_id"));
                            callTime = callTime - surplus_minute;
                        }
                    }
                }
                //如果所有套餐用完,电话还没有打完
                if (callTime > 0) {
                    fee = 0.5f * callTime;
                }
                execute("insert into callRecord(u_id,end_time,call_time,fee,wayOfCall) value(" + u_id + ",'" + sdf.format(new Date()) + "'," + String.valueOf(actual_callTime) + "," + String.valueOf(fee) + ",'拨打');");
                System.out.println("通话结束！");
                //扣除用户费用
                ResultSet resultSet1 = executeQuery("select * from user where u_id=" + u_id);
                float amount = 0;
                while (resultSet1.next()) {
                    amount = Float.parseFloat(resultSet1.getString("amount"));
                }
                execute("update user set amount=" + String.valueOf(amount - fee) + " where u_id=" + u_id);
                System.out.println("已扣除费用!");
            }else{  //接听免费
                execute("insert into callRecord(u_id,end_time,call_time,fee,wayOfCall) value(" + u_id + ",'" + sdf.format(new Date()) + "'," + String.valueOf(actual_callTime) + ",0,'接听');");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void useTrafficInTheLocalArea(String phoneNumber, float traffic){
        try {
            float actualTraffic = traffic;
            String u_id = getUserIdByPhoneNumber(phoneNumber);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            ResultSet resultSet = executeQuery("select * from subscription where u_id="+u_id+" and s_state='正在进行中' and localTraffic<>0");
            float fee = 0;
            while(resultSet.next()){
                float localTraffic = Float.valueOf(resultSet.getString("localTraffic"));
                if(traffic<=localTraffic){  //可从本地流量套餐中扣取
                    execute("update subscription set localTraffic=" + String.valueOf(localTraffic - traffic) + " where s_id=" + resultSet.getString("s_id"));
                    traffic = 0;
                    continue;
                }else{
                    execute("update subscription set localTraffic=0 where s_id=" + resultSet.getString("s_id"));
                    traffic = traffic-localTraffic;
                }
            }
            if(traffic>0){  //本地流量套餐用完，可继续使用国内套餐
                ResultSet resultSet1 = executeQuery("select * from subscription where u_id="+u_id+" and s_state='正在进行中' and domesticTraffic<>0");
                traffic = useDomesticPackage(traffic,resultSet1);
            }
            if(traffic>0){
                fee = traffic*2*1024;
            }
            execute("insert into trafficRecord(u_id,local_traffic,end_time,fee) value("+u_id+","+actualTraffic+",'"+sdf.format(new Date())+"',"+fee+");");
            System.out.println("流量使用结束！");
            //扣除用户费用
            ResultSet resultSet2 = executeQuery("select * from user where u_id=" + u_id);
            float amount = 0;
            while (resultSet2.next()) {
                amount = Float.parseFloat(resultSet2.getString("amount"));
            }
            execute("update user set amount=" + String.valueOf(amount - fee) + " where u_id=" + u_id);
            System.out.println("已扣除费用!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void useTrafficInTheField(String phoneNumber, float traffic){
        try {
            float actualTraffic = traffic;
            String u_id = getUserIdByPhoneNumber(phoneNumber);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            ResultSet resultSet = executeQuery("select * from subscription where u_id="+u_id+" and s_state='正在进行中' and domesticTraffic<>0");
            float fee = 0;
            traffic = useDomesticPackage(traffic,resultSet);
            if(traffic>0){
                fee = traffic*5*1024;
            }
            execute("insert into trafficRecord(u_id,domestic_traffic,end_time,fee) value("+u_id+","+actualTraffic+",'"+sdf.format(new Date())+"',"+fee+");");
            System.out.println("流量使用结束！");
            //扣除用户费用
            ResultSet resultSet2 = executeQuery("select * from user where u_id=" + u_id);
            float amount = 0;
            while (resultSet2.next()) {
                amount = Float.parseFloat(resultSet2.getString("amount"));
            }
            execute("update user set amount=" + String.valueOf(amount - fee) + " where u_id=" + u_id);
            System.out.println("已扣除费用!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void message(String phoneNumber){
        try {
            String u_id = getUserIdByPhoneNumber(phoneNumber);
            int message = 1;
            ResultSet resultSet = executeQuery("select * from subscription where u_id="+u_id+" and s_state='正在进行中' and surplus_message<>0");
            while(resultSet.next()){
                int surplus_message = Integer.parseInt(resultSet.getString("surplus_message"));
                if(surplus_message >= 1){
                    execute("update subscription set surplus_message=" + String.valueOf(surplus_message-1) + " where s_id=" + resultSet.getString("s_id"));
                    message = 0;
                    continue;
                }
            }
            ResultSet resultSet1 = executeQuery("select * from messageRecord where u_id="+u_id+";");
            int recordMessage = 0;
            double fee = 0;
            while(resultSet1.next()){
                recordMessage = Integer.parseInt(resultSet1.getString("messageNumber"));
                fee = Double.parseDouble(resultSet1.getString("fee"));
            }
            if(message > 0) {   //需要付短信费
                execute("update messageRecord set messageNumber=" + String.valueOf(recordMessage + 1) + ",fee=" + String.valueOf(fee + 0.1 * 1) + " where u_id=" + u_id);
                //扣除用户费用
                ResultSet resultSet2 = executeQuery("select * from user where u_id=" + u_id);
                double amount = 0;
                while (resultSet2.next()) {
                    amount = Double.parseDouble(resultSet2.getString("amount"));
                }
                execute("update user set amount=" + String.valueOf(amount - 0.1 * 1) + " where u_id=" + u_id);
                System.out.println("已扣除短信费用");
            }else{
                execute("update messageRecord set messageNumber=" + String.valueOf(recordMessage + 1)+ " where u_id=" + u_id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createBill(String phoneNumber){
        try {
            String u_id = getUserIdByPhoneNumber(phoneNumber);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            System.out.println("您的月账单生成如下");
            System.out.print("本月打电话的时间： ");
            ResultSet resultSet = executeQuery("select * from callRecord where u_id="+u_id);
            int callTime = 0;
            double callFee = 0;
            while(resultSet.next()){
                if(compare_date(resultSet.getString("end_time"),sdf.format(new Date()))==1){
                    callTime += Integer.parseInt(resultSet.getString("call_time"));
                    callFee += Double.parseDouble(resultSet.getString("fee"));
                }
            }
            System.out.println(callTime+"分钟    打电话花费的费用： "+callFee);

            ResultSet resultSet1 = executeQuery("select * from trafficRecord where u_id="+u_id);
            double localTraffic = 0;
            double localFee = 0;
            double domesticTraffic = 0;
            double domesticFee = 0;
            while(resultSet1.next()){
                if(compare_date(resultSet1.getString("end_time"),sdf.format(new Date()))==1){
                    localTraffic += Double.parseDouble(resultSet1.getString("local_traffic"));
                    domesticTraffic +=Double.parseDouble(resultSet1.getString("domestic_traffic"));
                    if(!resultSet1.getString("local_traffic").equals("0")){
                        localFee += Double.parseDouble(resultSet1.getString("fee"));
                    }
                    if(!resultSet1.getString("domestic_traffic").equals("0")){
                        domesticFee += Double.parseDouble(resultSet1.getString("fee"));
                    }
                }
            }
            System.out.println("每月使用的本地流量： "+localTraffic+"   花费费用： "+localFee);
            System.out.println("每月使用的国内流量： "+domesticTraffic+"   花费费用： "+domesticFee);
            System.out.println("您正在使用的流量套餐:");
            ResultSet resultSet2 = executeQuery("select * from subscription where u_id="+u_id+" and s_state='正在进行中'");
            while(resultSet2.next()){
                String str = getConcreteSubscription(resultSet2.getString("p_id"));//套餐总值
                ResultSet resultSet3 = executeQuery("select * from package where p_id="+resultSet2.getString("p_id"));
                String p_type = "";
                while(resultSet3.next()){
                    p_type = resultSet3.getString("p_type");
                }
                if(p_type.equals("话费套餐")){
                    double useMinute = Double.parseDouble(str)-Double.parseDouble(resultSet2.getString("surplus_phoneminute"));
                    System.out.println("您已使用套餐中的话费"+useMinute+"分钟    剩余套餐话费:"+Double.parseDouble(resultSet2.getString("surplus_phoneminute"))+"分钟");
                }else if(p_type.equals("短信套餐")){
                    double useMessage = Double.parseDouble(str)-Double.parseDouble(resultSet2.getString("surplus_message"));
                    System.out.println("您已使用套餐中的短信"+useMessage+"条    剩余套餐短信"+Double.parseDouble(resultSet2.getString("surplus_message"))+"条");
                }else if(p_type.equals("本地流量套餐")){
                    double useLocalTraffic = Double.parseDouble(str)-Double.parseDouble(resultSet2.getString("localTraffic"));
                    System.out.println("您已使用套餐中的本地流量"+useLocalTraffic+"G    剩余套餐本地浏览量"+Double.parseDouble(resultSet2.getString("localTraffic"))+"G");
                }else{
                    double useDomesticTraffic = Double.parseDouble(str)-Double.parseDouble(resultSet2.getString("domesticTraffic"));
                    System.out.println("您已使用套餐中的国内流量"+useDomesticTraffic+"G    剩余套餐国内浏览量"+Double.parseDouble(resultSet2.getString("domesticTraffic"))+"G");
                }
//                double useMinute = Double.parseDouble(str)-Double.parseDouble(resultSet2.getString("surplus_phoneminute"));//已使用的
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void showAllPackage(){
        try{
            ResultSet resultSet = executeQuery("select * from package");
            while(resultSet.next()){
                getConcreteSubscription(resultSet.getString("p_id"));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * 准确查询
     * @param sql
     * @return
     */
    public ResultSet executeQuery(String sql){
        PreparedStatement prst = null;
        try {
            prst = connection.prepareStatement(sql);
            return prst.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 执行sql语句
     * @param sql
     * @return
     */
    public boolean execute(String sql){
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            boolean isExecute = preparedStatement.execute();
            return isExecute;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据套餐id查询出具体套餐
     * @param p_id
     */
    private String getConcreteSubscription(String p_id) throws SQLException {
        System.out.println("套餐具体内容");
        ResultSet resultSet = executeQuery("select * from package where p_id="+p_id);
        int index = 0;
        String res = "";
        while(resultSet.next()){
            List<String> volumes = Arrays.asList("月功能费","最多可拨打分钟数","超出时间按照x元/分钟计费","最多可发送短信数","超出条数按x元/条计费","最多可获得多少G本地流量","超出本地流量按x元/M计费","最多可获得多少G国内流量","超出国内流量按x元/M计费");
            System.out.print("套餐Id: "+resultSet.getString("p_id")+"   套餐名称："+resultSet.getString("p_name")+"   ");
            for(int i=0; i<9; i++){
                String str = resultSet.getString(i+3);
                if(!str.equals("0")){
                    index++;
                    System.out.print(volumes.get(i)+":"+str+"   ");
                    if(index == 2){
                        res = resultSet.getString(i+3);
                    }
                }
            }
            System.out.println();
        }
        return res;
    }

    /**
     * 根据电话号码寻找用户编号
     * @param phoneNumber
     * @return
     * @throws SQLException
     */
    private String getUserIdByPhoneNumber(String phoneNumber) throws SQLException {
        ResultSet rs_1 = executeQuery("select * from user where phone='"+phoneNumber+"'");
        String u_id = "";
        while (rs_1.next()) {
            u_id = rs_1.getString("u_id");
        }
        return u_id;
    }

    /**
     * 比较日期先后
     * @param date1
     * @param date2
     * @return
     * @throws ParseException
     */
    private int compare_date(String date1, String date2) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        Date t1 = sdf.parse(date1);
        Date t2 = sdf.parse(date2);
        if(t1.getTime()<t2.getTime()){
//            System.out.println("t1 在t2前");
            return 1;
        }else if(t1.getTime()>t2.getTime()){
//            System.out.println("t1 在t2后");
            return -1;
        }else{
            return 0;
        }
    }

    /**
     * 得到套餐优惠的具体内容
     * @param p_id
     * @return
     * @throws SQLException
     */
    private List<Double> getPackageContent(String p_id) throws SQLException {
        List<Double> list = new ArrayList<Double>();
        ResultSet resultSet = executeQuery("select * from package where p_id="+p_id);
        double allValue = 0;
        double functional_fee = 0;
        while(resultSet.next()){
            String p_type = resultSet.getString("p_type");
            if(p_type.equals("话费套餐")){
                allValue = Double.parseDouble(resultSet.getString("phone_minute"));
            }else if(p_type.equals("短信套餐")){
                allValue = Double.parseDouble(resultSet.getString("message_number"));
            }else if(p_type.equals("本地流量套餐")){
                allValue = Double.parseDouble(resultSet.getString("localTraffic"));
            }else{
                allValue = Double.parseDouble(resultSet.getString("domesticTraffic"));
            }
            functional_fee = Double.parseDouble(resultSet.getString("functional_fee"));
        }
        list.add(allValue);
        list.add(functional_fee);
        return list;
    }

    private double getSubscriptionContent(String u_id, String p_id) throws SQLException{
        ResultSet resultSet = executeQuery("select * from subscription where p_id="+p_id+" and u_id="+u_id+" and s_state='正在进行中';");
        double value = 0;
        while(resultSet.next()){
            double minute = Double.parseDouble(resultSet.getString("surplus_phoneminute"));
            double message = Double.parseDouble(resultSet.getString("surplus_message"));
            double localTraffic = Double.parseDouble(resultSet.getString("localTraffic"));
            double domesticTraffic = Double.parseDouble(resultSet.getString("domesticTraffic"));
            if(minute!=0){
                value = minute;
            }else if(message!=0){
                value = message;
            }else if(localTraffic!=0){
                value = localTraffic;
            }else{
                value = domesticTraffic;
            }
        }
        return value;
    }

    /**
     * 使用国内流量套餐
     * @param traffic
     * @param resultSet
     * @return
     * @throws SQLException
     */
    private float useDomesticPackage(float traffic, ResultSet resultSet) throws SQLException{
        while(resultSet.next()){
            float domesticTraffic = Float.valueOf(resultSet.getString("domesticTraffic"));
            if(traffic<=domesticTraffic){
                execute("update subscription set domesticTraffic=" + String.valueOf(domesticTraffic - traffic) + " where s_id=" + resultSet.getString("s_id"));
                traffic = 0;
                continue;
            }else{
                execute("update subscription set domesticTraffic=0 where s_id=" + resultSet.getString("s_id"));
                traffic = traffic-domesticTraffic;
            }
        }
        return traffic;
    }

}
