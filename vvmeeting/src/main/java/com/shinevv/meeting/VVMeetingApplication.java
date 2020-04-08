package com.shinevv.meeting;

import com.shinevv.data.ServerAPI;
import com.shinevv.vvroom.VVRoomApplication;

/**
 * @createDate: 2020/10/08 14:48
 * @author: houtong
 * @email: houtyim@gamil.com
 */
public class VVMeetingApplication extends VVRoomApplication {

    //updata servedr api
    public static void upDataAPI(String str) {
        serverAPI = getRetrofitApi(str, ServerAPI.class);
    }
}
