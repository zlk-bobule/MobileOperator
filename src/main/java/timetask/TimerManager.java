package timetask;

import dataservice.Impl.OperationDataServiceImpl;
import dataservice.OperationDataService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 定时任务
 * 每月初执行
 * 目标在于改变订阅状态，扣取用户余额
 */

public class TimerManager {
    public TimerManager(Connection connection){
        Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.MONTH,1);
        calendar.set(Calendar.DATE,1);
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);


        Date date = calendar.getTime();
        System.out.println(date);
        Timer timer = new Timer();
        final OperationDataService operationDataService = new OperationDataServiceImpl(connection);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                operationDataService.execute("update subscription set s_state='正在进行中' where s_state='即将生效';");
                operationDataService.execute("update subscription set s_state='已失效' where unsubscribe_time is not null and s_state='正在进行中';");
                ResultSet resultSet = operationDataService.executeQuery("select * from subscription where s_state='正在进行中';");
                try{
                    while(resultSet.next()){
                        String u_id = resultSet.getString("u_id");
                        String p_id = resultSet.getString("p_id");
                        double functionalFee = 0;
                        ResultSet resultSet1 = operationDataService.executeQuery("select * from package where p_id="+p_id);
                        String p_type = "";
                        while(resultSet1.next()){
                            p_type = resultSet1.getString("p_type");
                            functionalFee = Double.parseDouble(resultSet1.getString("functional_fee"));
                            if(p_type.equals("话费套餐")){
                                double minute = Double.parseDouble(resultSet1.getString("phone_minute"));
                                operationDataService.execute("update subscription set surplus_phoneminute="+minute+" where u_id="+u_id+" and p_id="+p_id+" and s_state='正在进行中'");
                            }else if(p_type.equals("短信套餐")){
                                double message = Double.parseDouble(resultSet1.getString("message_number"));
                                operationDataService.execute("update subscription set surplus_message="+message+" where u_id="+u_id+" and p_id="+p_id+" and s_state='正在进行中'");
                            }else if(p_type.equals("本地流量套餐")){
                                double localTraffic = Double.parseDouble(resultSet1.getString("localTraffic"));
                                operationDataService.execute("update subscription set localTraffic="+localTraffic+" where u_id="+u_id+" and p_id="+p_id+" and s_state='正在进行中'");
                            }else{
                                double domesticTraffic = Double.parseDouble(resultSet1.getString("domesticTraffic"));
                                operationDataService.execute("update subscription set domesticTraffic="+domesticTraffic+" where u_id="+u_id+" and p_id="+p_id+" and s_state='正在进行中'");
                            }
                            ResultSet resultSet2 = operationDataService.executeQuery("select * from user where u_id="+u_id);
                            while (resultSet2.next()){
                                double amount = Double.parseDouble(resultSet2.getString("amount"));
                                operationDataService.execute("update user set amount="+String.valueOf(amount-functionalFee)+" where u_id="+u_id);
                            }
                        }
                    }
                }catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(task,date);
    }
}
