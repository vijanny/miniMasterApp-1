package com.masterdroup.minimasterapp.module.welcomeModule;


import android.support.annotation.Nullable;
import android.util.Log;

import com.gizwits.gizwifisdk.api.GizWifiSDK;
import com.gizwits.gizwifisdk.enumration.GizUserAccountType;
import com.masterdroup.minimasterapp.App;
import com.masterdroup.minimasterapp.Constant;
import com.masterdroup.minimasterapp.R;
import com.masterdroup.minimasterapp.api.Network;
import com.masterdroup.minimasterapp.model.Base;
import com.masterdroup.minimasterapp.model.Null;
import com.masterdroup.minimasterapp.model.PhoneAndToken;
import com.masterdroup.minimasterapp.model.Token;
import com.masterdroup.minimasterapp.model.User;
import com.masterdroup.minimasterapp.module.progress.ProgressSubscriber;
import com.masterdroup.minimasterapp.util.DebugUtils;
import com.masterdroup.minimasterapp.util.JxUtils;
import com.masterdroup.minimasterapp.util.StringFormatCheckUtils;
import com.masterdroup.minimasterapp.util.ToastUtils;
import com.masterdroup.minimasterapp.util.Utils;
import com.yuyh.library.imgsel.utils.LogUtils;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by 11473 on 2016/11/29.
 */

public class WelcomePresenter implements Contract.Presenter {


    Contract.MainView mainView;
    Contract.LoginView loginView;
    Contract.RegisteredView registeredView;
    Contract.RetrievePwdView retrievePwdView;


    public WelcomePresenter(Contract.MainView mView) {
        this.mainView = mView;
        mainView = Utils.checkNotNull(mView, "mView cannot be null!");
        mainView.setPresenter(this);
    }

    public WelcomePresenter(Contract.LoginView mView) {
        this.loginView = mView;
        loginView = Utils.checkNotNull(mView, "mView cannot be null!");
        loginView.setPresenter(this);
    }

    public WelcomePresenter(Contract.RegisteredView mView) {
        this.registeredView = mView;
        registeredView = Utils.checkNotNull(mView, "mView cannot be null!");
        registeredView.setPresenter(this);
    }

    public WelcomePresenter(Contract.RetrievePwdView mView) {
        this.retrievePwdView = mView;
        retrievePwdView = Utils.checkNotNull(mView, "mView cannot be null!");
        retrievePwdView.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void showLoginView() {
        mainView.onShowLoginView();
    }

    @Override
    public void showRegisteredView() {
        mainView.onRegisteredView();
    }

    @Override
    public void showMainView() {
        mainView.onShowMainView();
    }

    @Override
    public boolean isLogin() {

        //判读token值是否存在
        try {

            return App.spUtils.contains(App.mContext.getString(R.string.key_token));

        } catch (Exception e) {
            return false;
        }


    }

    /** 登陆机智云成功后，将信息抛给我们的服务器 */
    @Override
    public void login(final String name, String pwd, final String giz_uid, final String giz_token) {
        DebugUtils.d("WelcomePresenter", "login()");

        if (name.isEmpty() || pwd.isEmpty()) {
            loginView.onLoginFailure(App.mContext.getString(R.string.err_login_1));
        } else {

            User user = new User();
            User.UserBean userBean = user.new UserBean();
            userBean.setName(name);
            userBean.setPassword(pwd);
            user.setUser(userBean);

            Observable observable = Network.getMainApi().login(user);

            Subscriber<Base<Token>> s = new ProgressSubscriber<>(new ProgressSubscriber.SubscriberOnNextListener<Base<Token>>() {
                @Override
                public void onNext(Base<Token> o) {
                    if (o.getErrorCode() == 0)
                        loginView.onLoginSuccess(name,o.getRes().getToken(), giz_uid, giz_token);
                    else {
                        loginView.onLoginFailure(o.getMessage());
                    }
                }

            }, loginView.onGetContext());


            JxUtils.toSubscribe(observable, s);
        }


    }


    @Override
    public void gizLogin(String nameOrPhone, final String pwd) {
        if(StringFormatCheckUtils.isPhoneLegal(nameOrPhone)) {
            LoginFragment.PHONE_NUMBER_LOGIN = true;
            GizWifiSDK.sharedInstance().userLogin(nameOrPhone, pwd);
        } else {
            LoginFragment.PHONE_NUMBER_LOGIN = false;
            int nicknameResult = StringFormatCheckUtils.isNickNameLegal(nameOrPhone);
            if(nicknameResult == R.string.nickname_right) {
                // GizWifiSDK.sharedInstance().userLogin(nameOrPhone, pwd);
                User user =  new User();
                User.UserBean userBean = user.new UserBean();
                userBean.setName(nameOrPhone);
                userBean.setPassword(pwd);
                user.setUser(userBean);

                Observable observable = Network.getMainApi().signin(user);
                Subscriber<Base<PhoneAndToken>> s = new ProgressSubscriber(new ProgressSubscriber.SubscriberOnNextListener<Base<PhoneAndToken>>(){

                    @Override
                    public void onNext(Base<PhoneAndToken> o) {
                        if(o.getErrorCode() == 0) {
                            loginView.onLoginSmartNetwork(o.getRes().getPhone(), pwd, o.getRes().getToken());
                        } else {
                            loginView.onLoginFailure(o.getMessage());
                        }
                    }
                }, loginView.onGetContext()
                );

                JxUtils.toSubscribe(observable, s);

            } else {
                ToastUtils.showCustomToast(App.mContext, ToastUtils.TOAST_CENTER, App.mContext.getString(nicknameResult));
            }
        }

    }


    @Override
    public void registered(String nickName, String password, @Nullable String phoneNum, String uid) {
        DebugUtils.d("WelcomePresenter", "registered()");

        if (nickName.isEmpty() || password.isEmpty()) {
            registeredView.onRegisteredFailure(App.mContext.getString(R.string.registered_f));
        } else {

            User user = new User();
            User.UserBean userBean = user.new UserBean();
            userBean.setName(nickName);
            userBean.setPassword(password);
            userBean.setPhoneNum(phoneNum);
            userBean.setUid(uid);
            user.setUser(userBean);

            Observable observable = Network.getMainApi().registered(user);
            Subscriber<Base<Null>> s = new ProgressSubscriber<>(new ProgressSubscriber.SubscriberOnNextListener<Base<Null>>() {
                @Override
                public void onNext(Base<Null> o) {
                    if (o.getErrorCode() == 0)
                        registeredView.onRegisteredSuccess();
                    else {
                        registeredView.onRegisteredFailure(o.getMessage());
                    }
                }

            }, registeredView.onGetContext());

            JxUtils.toSubscribe(observable, s);
        }

    }

    @Override
    public void gizRegistered(String nickName, String phoneNum, String ver, String pwd, String againPwd) {
        // 昵称检查
        int nickNameResult =  StringFormatCheckUtils.isNickNameLegal(nickName);
        if(nickNameResult != R.string.nickname_right) {
            showRegisterToast(App.mContext.getString(nickNameResult));
            return;
        }
        // 号码检查
        if( !(StringFormatCheckUtils.isPhoneLegal(phoneNum)) ) {
            showRegisterToast(App.mContext.getString(R.string.phone_wrong_format));
            return;
        }

        //验证码检查
        int verificationResult = StringFormatCheckUtils.isVerificationLegal(ver);
        if(verificationResult != R.string.verification_right) {
            showRegisterToast(App.mContext.getString(verificationResult));
            return ;
        }

        //密码检测
        int pwdResult = StringFormatCheckUtils.isPasswordLegal(pwd);
        if(pwdResult != R.string.pwd_right) {
            showRegisterToast(App.mContext.getString(pwdResult));
            return;
        }

        //再次输入密码检测
        int againPwdResult = StringFormatCheckUtils.isAgainPwdLegal(pwd, againPwd);
        if(againPwdResult != R.string.again_pwd_right) {
            showRegisterToast(App.mContext.getString(againPwdResult));
            return ;
        }

        LogUtils.d("gizRegistered", "注册参数检测通过");
        GizWifiSDK.sharedInstance().registerUser(phoneNum, pwd, ver, GizUserAccountType.GizUserPhone);

    }


    /** 获取验证码 */
    @Override
    public void sendPhoneSMSCode(String phone) {

        if(StringFormatCheckUtils.isPhoneLegal(phone)) {//非法检查

            GizWifiSDK.sharedInstance().requestSendPhoneSMSCode(Constant.APP_Secret, phone);

        } else {
            Log.d("sendPhoneSMSCode", "号码格式有误！");
            showRegisterToast( App.mContext.getString(R.string.phone_wrong_format));
        }
    }

    /** 参数合法性检测和改机智云端用户密码 */
    @Override
    public void CheckRetrievePwdPara(String phoneNum, String verification, String newPwd, String againNewPwd) {
        // 号码检查
        if( !(StringFormatCheckUtils.isPhoneLegal(phoneNum)) ) {
            showRegisterToast(App.mContext.getString(R.string.phone_wrong_format));
            return;
        }

        //验证码检查
        int verificationResult = StringFormatCheckUtils.isVerificationLegal(verification);
        if(verificationResult != R.string.verification_right) {
            showRegisterToast(App.mContext.getString(verificationResult));
            return ;
        }

        //密码检测
        int pwdResult = StringFormatCheckUtils.isPasswordLegal(newPwd);
        if(pwdResult != R.string.pwd_right) {
            showRegisterToast(App.mContext.getString(pwdResult));
            return;
        }

        //再次输入密码检测
        int againPwdResult = StringFormatCheckUtils.isAgainPwdLegal(newPwd, againNewPwd);
        if(againPwdResult != R.string.again_pwd_right) {
            showRegisterToast(App.mContext.getString(againPwdResult));
            return ;
        }

        LogUtils.d("gizRegistered", "注册参数检测通过");
        GizWifiSDK.sharedInstance().resetPassword(phoneNum, verification, newPwd, GizUserAccountType.GizUserPhone);
    }

    /** */
    @Override
    public void Retrieve(String phoneNum, String newPwd) {

        User user = new User();
        User.UserBean userBean = user.new UserBean();
        userBean.setPhoneNum(phoneNum);
        userBean.setPassword(newPwd);
        user.setUser(userBean);

        Observable observable = Network.getMainApi().login(user);

        Subscriber<Base<Token>> s = new ProgressSubscriber<>(new ProgressSubscriber.SubscriberOnNextListener<Base<Token>>() {
            @Override
            public void onNext(Base<Token> o) {

            }

        }, loginView.onGetContext());


        JxUtils.toSubscribe(observable, s);
        /**
         *  User user = new User();
         User.UserBean userBean = user.new UserBean();
         userBean.setName(nickName);
         userBean.setPassword(password);
         userBean.setPhoneNum(phoneNum);
         userBean.setUid(uid);
         user.setUser(userBean);

         Observable observable = Network.getMainApi().registered(user);
         Subscriber<Base<Null>> s = new ProgressSubscriber<>(new ProgressSubscriber.SubscriberOnNextListener<Base<Null>>() {
        @Override
        public void onNext(Base<Null> o) {
        if (o.getErrorCode() == 0)
        registeredView.onRegisteredSuccess();
        else {
        registeredView.onRegisteredFailure(o.getMessage());
        }
        }

        }, registeredView.onGetContext());

         JxUtils.toSubscribe(observable, s);
         * */
    }


    private void showRegisterToast(String info) {
        ToastUtils.showCustomToast(App.mContext, ToastUtils.TOAST_CENTER, info);
    }

}





