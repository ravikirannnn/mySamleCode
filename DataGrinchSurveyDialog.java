package com.cloud.datagrinchsdk.utils.survey;

//Import headers here...
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.cloud.datagrinchsdk.R;
import com.cloud.datagrinchsdk.utils.applicationutils.AppConstant;
import com.cloud.datagrinchsdk.utils.applicationutils.AppUtils;
import com.cloud.datagrinchsdk.utils.applicationutils.LoggerUtils;
import com.cloud.datagrinchsdk.utils.applicationutils.PreferenceUtils;
import com.cloud.datagrinchsdk.utils.db.DBManager;
import com.cloud.datagrinchsdk.utils.db.SurveyDataTable;
import com.cloud.datagrinchsdk.utils.networkutils.DataGrinchNetworkTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This dialog class is used to display Data Grinch survey dialogs on the
 * application UI.
 *
 * @author Moses.Kesavan
 */
@SuppressLint("InlinedApi")
public class DataGrinchSurveyDialog extends DialogFragment {

    /**
     * Constant to hold the type of dialog to be rendered
     */
    private static final int DIALOG_WELCOME_SCREEN = 1;
    private static final int DIALOG_QUESTION_SCREEN = 2;
    private static final int DIALOG_THANK_YOU_SCREEN = 3;

    /**
     * holds the current dialog ID
     */
    private int mId = -1;

    /**
     * Maintains a global survey data counnt
     */
    int mSurveyDataSize = 0;

    /**
     * Maintains a global survey question count
     */
    int mQuestionCount = 0;

    /**
     * Holds the layout containing the child views
     */
    View mChildLayoutView = null;

    /**
     * Holds the layout containing the parent views
     */
    private FrameLayout mParentFrameLayout;

    /**
     * Holds array list of survey objects
     */
    private ArrayList<SurveyDataTable> mSurveyDataList;

    /**
     * Holds array list of survey question
     */
    private ArrayList<String> mSurveyAnswerObject;

    /**
     * Holds header text view
     */
    private TextView mHeaderText;

    /**
     * Holds close survey text view
     */
    private TextView mCloseSurveyButton;

    /**
     * Holds close skip text view
     */
    private TextView mSkipText;

    /**
     * Holds survey questions text view
     */
    private TextView mSurveyQuestion;

    /**
     * Holds radio group which renders options
     */
    private RadioGroup mOptionRadioGroup;

    /**
     * Holds custom edit view to write the anser
     */
//    private LineEditText mAnswerEditText;
    private LineEditText mAnswerEditText;

    /**
     * Layout inflater to inflate different UI
     */
    private LayoutInflater mInflater;

    /**
     * Holds th custom progress bar instance
     */
    private ProgressBar mSurveyProgressBar;

    /**
     * Holds next button instance
     */
    private Button mNextButton;

    /**
     * Holds single object of survey data
     */
    private SurveyDataTable mSurveyDataObject;

    /**
     * Holds global instance of radio button
     */
    private RadioButton mPrevSelectedButton;

    /**
     * Holds global instance tick image view
     */
    private ImageView mOptionTypeImageView;

    /**
     * Fragment dialog instance
     */
    private Dialog mDialog;

    private Context context;

    private ArrayList<SurveyDataTable> mResponseData;

    private boolean isExpired;

    private CountDownTimer sCountDownTimer;


    public DataGrinchSurveyDialog() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        LoggerUtils.LogError("onCreateVIew", "creating the view !");
        return initComponents(inflater);
    }

    @Override
    public void onAttach(Activity activity) {
        context = activity;
//        LoggerUtils.LogError("onAttach", "I am in Attaching");
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sCountDownTimer != null)
            sCountDownTimer.cancel();
        if (isExpired) {
            isExpired = false;
//            LoggerUtils.LogError("In resume", "I am in resume");
            getActivity().getSupportFragmentManager().beginTransaction().remove(DataGrinchSurveyDialog.this).commit();
        }

    }


    @Override
    public void onStop() {
        super.onStop();
//        LoggerUtils.LogError("In Stop", "I am in Stop");
        long userProvidedTime = PreferenceUtils.getLongFromPreference(
                context, AppConstant.USER_PROVIDED_TIME);

        if (userProvidedTime < 0) {
            userProvidedTime = AppConstant.DEFAULT_BACKGROUND_TIME
                    * AppConstant.TIME_MULTIPLIER;
        } else {
            userProvidedTime = userProvidedTime
                    * AppConstant.TIME_MULTIPLIER;
        }
        sCountDownTimer = new CountDownTimer(userProvidedTime, AppConstant.TIME_MULTIPLIER) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                isExpired = true;
//                LoggerUtils.LogError("Expired", "Expired !!!!!!");
            }
        }.start();

    }

    /**
     * Method to initialise all the view and their respective data and return it
     * to Dialog.
     *
     * @param inflater
     * @return
     */
    private View initComponents(LayoutInflater inflater) {

        mDialog = getDialog();
        mDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setCancelable(false);

        /**
         * Fetching the dialog and setting its width height to fit the screen
         */
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(mDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        mDialog.getWindow().setAttributes(lp);

        View view = null;
        mInflater = inflater;
        view = mInflater.inflate(R.layout.survey_dialog, null);
        mParentFrameLayout = (FrameLayout) view
                .findViewById(R.id.parent_frame_layout);
        mSurveyDataList = SurveyDataTable.loadSurveyData(context);
        mResponseData = new ArrayList<>();
        renderDialogs(DIALOG_WELCOME_SCREEN);
        return view;
    }

    /**
     * Method to render different layouts for different dialog types.
     *
     * @param dialogType
     */
    private void renderDialogs(int dialogType) {
        TextView messageText = null;
        TextView thankyouHeader = null;
        SurveyDataTable surveyDataTable = mSurveyDataList != null ? mSurveyDataList.get(0) : null;
        switch (dialogType) {

            case DIALOG_WELCOME_SCREEN:

                mChildLayoutView = mInflater.inflate(R.layout.survey_error_dialog,
                        null);
                if (surveyDataTable != null) {
                    mChildLayoutView.findViewById(R.id.header_layout).setBackgroundColor(Color.parseColor(surveyDataTable.getBaseColor()));
                    mChildLayoutView.findViewById(R.id.welcome_screen_footer_button_layout).setBackgroundColor(Color.parseColor(surveyDataTable.getBaseColor()));
                }
                mChildLayoutView.findViewById(
                        R.id.welcome_screen_footer_button_layout).setVisibility(
                        View.VISIBLE);
                Button cancelButton = (Button) mChildLayoutView
                        .findViewById(R.id.cancel_button);
                cancelButton.setBackground(setCustomDrawable(surveyDataTable));

                Button proceedButton = (Button) mChildLayoutView
                        .findViewById(R.id.proceed_button);
                proceedButton.setBackground(setCustomDrawable(surveyDataTable));
                thankyouHeader = (TextView) mChildLayoutView
                        .findViewById(R.id.survey_dialog_header_title_text);
                thankyouHeader.setText(context
                        .getString(R.string.header_title_welcome));
                messageText = (TextView) mChildLayoutView
                        .findViewById(R.id.message_text_view);
                messageText.setText(surveyDataTable.getWelcomeMessage());
                cancelButton.setOnClickListener(mMesssageDialogButtonClickListener);
                proceedButton
                        .setOnClickListener(mMesssageDialogButtonClickListener);
                mParentFrameLayout.removeAllViews();
                mParentFrameLayout.addView(mChildLayoutView);

                break;
            case DIALOG_QUESTION_SCREEN:
                if (mSurveyDataList != null && mSurveyDataList.size() > 0) {
                    mChildLayoutView = mInflater.inflate(
                            R.layout.dilaog_type_question, null);

                    if (surveyDataTable != null) {
                        mChildLayoutView.findViewById(R.id.header_layout).setBackgroundColor(Color.parseColor(surveyDataTable.getBaseColor()));
                        mChildLayoutView.findViewById(R.id.footer_button_layout).setBackgroundColor(Color.parseColor(surveyDataTable.getBaseColor()));
                    }
                    mHeaderText = (TextView) mChildLayoutView
                            .findViewById(R.id.survey_dialog_header_title_text);
                    mCloseSurveyButton = (TextView) mChildLayoutView
                            .findViewById(R.id.survey_dialog_close_text);
                    mCloseSurveyButton
                            .setOnClickListener(mQuestionDialogButtonListener);
                    mSkipText = (TextView) mChildLayoutView
                            .findViewById(R.id.survey_skip_text_view);
                    mSkipText.setOnClickListener(mQuestionDialogButtonListener);
                    mNextButton = (Button) mChildLayoutView
                            .findViewById(R.id.next_button);
                    mNextButton.setBackground(setCustomDrawable(surveyDataTable));
                    mSurveyQuestion = (TextView) mChildLayoutView
                            .findViewById(R.id.survey_question_text_view);
                    mOptionRadioGroup = (RadioGroup) mChildLayoutView
                            .findViewById(R.id.survey_option_radio_radio_group);
                    mSurveyProgressBar = (ProgressBar) mChildLayoutView
                            .findViewById(R.id.ProgressBar);
                    mOptionTypeImageView = (ImageView) mChildLayoutView
                            .findViewById(R.id.option_icon_image);
                    mAnswerEditText = (LineEditText) mChildLayoutView
                            .findViewById(R.id.survey_answer_edit_text);
                    mAnswerEditText.addTextChangedListener(mTextChangeListener);
                    checkFieldsForEmptyValues();
                    inflateSurveyUI(context);

                }
                break;
            case DIALOG_THANK_YOU_SCREEN:

                mChildLayoutView = mInflater.inflate(R.layout.survey_error_dialog,
                        null);
                if (surveyDataTable != null) {
                    mChildLayoutView.findViewById(R.id.header_layout).setBackgroundColor(Color.parseColor(surveyDataTable.getBaseColor()));
                    mChildLayoutView.findViewById(R.id.error_screen_footer_button_layout).setBackgroundColor(Color.parseColor(surveyDataTable.getBaseColor()));
                }
                mChildLayoutView.findViewById(
                        R.id.welcome_screen_footer_button_layout).setVisibility(
                        View.GONE);
                mChildLayoutView.findViewById(
                        R.id.error_screen_footer_button_layout).setVisibility(
                        View.VISIBLE);
                messageText = (TextView) mChildLayoutView
                        .findViewById(R.id.message_text_view);
                messageText.setText(surveyDataTable.getThankYouMessage());
                Linkify.addLinks(messageText, Linkify.EMAIL_ADDRESSES);
                thankyouHeader = (TextView) mChildLayoutView
                        .findViewById(R.id.survey_dialog_header_title_text);
                thankyouHeader.setText(context
                        .getString(R.string.header_title_thank_you));
                Button okButton = (Button) mChildLayoutView
                        .findViewById(R.id.ok_button);
                okButton.setBackground(setCustomDrawable(surveyDataTable));
                okButton.setOnClickListener(mMesssageDialogButtonClickListener);
                mParentFrameLayout.removeAllViews();
                mParentFrameLayout.addView(mChildLayoutView);

                break;

            default:
                break;
        }

    }

    /**
     * Text watcher for the answer edit text to check if the edit box is empty or not.
     */
    private TextWatcher mTextChangeListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            checkFieldsForEmptyValues();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkFieldsForEmptyValues();
        }

        @Override
        public void afterTextChanged(Editable s) {
            checkFieldsForEmptyValues();
        }
    };

    /**
     * Method to inflate new views for building the dialog UI and rendering on
     * to the screen.
     *
     * @param context
     */
    public void inflateSurveyUI(Context context) {
        try {

            loadSurveyData();
            if (mSurveyAnswerObject != null) {
                mSurveyAnswerObject = null;
            }
            mSurveyAnswerObject = new ArrayList<String>();

            if (mSurveyDataSize > 0 && mQuestionCount <= mSurveyDataSize - 1) {
                mSurveyDataObject = null;
                mSurveyDataObject = mSurveyDataList.get(mQuestionCount);

                mSurveyQuestion.setText(mSurveyDataObject.getQuestion());
                mOptionRadioGroup
                        .setOnCheckedChangeListener(mRadioGroupListener);

                // Adding the radio group and radio button to show option:
                JSONArray optionArray = new JSONArray(mSurveyDataList.get(
                        mQuestionCount).getOptions());

                for (int i = 0; i < optionArray.length(); i++) {
                    JSONObject jsonObject = optionArray.getJSONObject(i);

                    if (mSurveyDataObject.getQuestionType().equalsIgnoreCase(
                            AppConstant.MCQ_MULTIPLE_ANSWER)) {

                        mAnswerEditText.setVisibility(View.GONE);
                        mOptionRadioGroup.setVisibility(View.VISIBLE);
                        mOptionTypeImageView.setVisibility(View.VISIBLE);

                        mOptionTypeImageView
                                .setBackgroundResource(R.drawable.check);
                        CheckBox optionCheckButton = (CheckBox) mInflater
                                .inflate(R.layout.survey_check_button, null);

                        optionCheckButton.setId(i);
                        optionCheckButton
                                .setOnCheckedChangeListener(mOptionCheckListener);
                        optionCheckButton.setText(jsonObject
                                .getString("option"));
                        optionCheckButton.setBackground(setCustomDrawable(mSurveyDataObject));

                        LayoutParams params = new LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.WRAP_CONTENT, 0);
                        optionCheckButton.setLayoutParams(params);
                        mOptionRadioGroup.addView(optionCheckButton, i);

                    } else if (mSurveyDataObject.getQuestionType()
                            .equalsIgnoreCase(AppConstant.MCQ_SINGLE_ANSWER)) {

                        mAnswerEditText.setVisibility(View.GONE);
                        mOptionRadioGroup.setVisibility(View.VISIBLE);
                        mOptionTypeImageView.setVisibility(View.VISIBLE);

                        mOptionTypeImageView
                                .setBackgroundResource(R.drawable.radio);
                        RadioButton optionRadioButton = (RadioButton) mInflater
                                .inflate(R.layout.survey_radio_button, null);

                        optionRadioButton.setId(i);
                        optionRadioButton.setText(jsonObject
                                .getString("option"));
                        optionRadioButton.setBackground(setCustomDrawable(mSurveyDataObject));
                        LayoutParams params = new LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.WRAP_CONTENT, 0);
                        optionRadioButton.setLayoutParams(params);
                        mOptionRadioGroup.addView(optionRadioButton, i);

                    } else if (mSurveyDataObject.getQuestionType()
                            .equalsIgnoreCase(AppConstant.ONE_WORD_ANSWER)) {
                        mAnswerEditText.setVisibility(View.VISIBLE);
                        mOptionRadioGroup.setVisibility(View.GONE);
                        mOptionTypeImageView.setVisibility(View.GONE);
                        mAnswerEditText.setLines(1);
                        mAnswerEditText.refreshDrawableState();

                    } else if (mSurveyDataObject.getQuestionType()
                            .equalsIgnoreCase(
                                    AppConstant.SHORT_DESCRIPTION_ANSWER)) {
                        mAnswerEditText.setVisibility(View.VISIBLE);
                        mOptionRadioGroup.setVisibility(View.GONE);
                        mOptionTypeImageView.setVisibility(View.GONE);
                        mAnswerEditText.setLines(5);

                    }

                }
                if (mQuestionCount == mSurveyDataSize - 1) {
                    mNextButton.setText(context.getString(
                            R.string.button_text_finish));
                } else {
                    mNextButton.setText(context.getString(
                            R.string.button_text_next));
                }
                mNextButton.setOnClickListener(mQuestionDialogButtonListener);
                mParentFrameLayout.removeAllViews();
                mParentFrameLayout.addView(mChildLayoutView);
                mParentFrameLayout.invalidate();
                mQuestionCount++;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Method to retrieve survey data from the table.
     */
    private void loadSurveyData() {
        mSurveyDataSize = -1;
        mSurveyDataSize = mSurveyDataList != null ? mSurveyDataList.size() : 0;
        mSurveyProgressBar.setMax(mSurveyDataSize);
        mSurveyProgressBar.setProgress(mQuestionCount);
        mHeaderText.setText(Html.fromHtml("<b>"
                + String.format(
                context.getString(
                        R.string.header_current_question_txt),
                mQuestionCount + 1) + "</b>")
                + AppConstant.SPACE
                + String.format(
                context.getString(
                        R.string.header_total_question_txt),
                mSurveyDataSize));
    }

    /**
     * Check listener for check boxes to save selected answer.
     */
    private CompoundButton.OnCheckedChangeListener mOptionCheckListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {

            if (isChecked) {
                mSurveyAnswerObject.add(buttonView.getText().toString());
                mNextButton.setEnabled(true);
                Drawable img = context.getResources().getDrawable(
                        R.drawable.tick);
                img.setBounds(0, 0, 20, 20);
                buttonView.setCompoundDrawables(null, null, img, null);

            } else {
                mSurveyAnswerObject.remove(buttonView.getText().toString());
                buttonView.setCompoundDrawables(null, null, null, null);
            }
        }
    };

    /**
     * Button and text click listener.
     */
    private OnClickListener mQuestionDialogButtonListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            mNextButton.setEnabled(false);

            if (v.getId() == R.id.next_button) {

                if (mSurveyDataObject.getQuestionType().equalsIgnoreCase(
                        AppConstant.MCQ_MULTIPLE_ANSWER)) {

                    int answerSize = mSurveyAnswerObject != null ? mSurveyAnswerObject
                            .size() : 0;

                    if (answerSize > 0) {
                        String selectedAnswers = "";

                        for (int answerIndex = 0; answerIndex < answerSize; answerIndex++) {
                            selectedAnswers += "," + mSurveyAnswerObject.get(answerIndex);
                        }
                        mSurveyDataObject.setAnswer(selectedAnswers.substring(1));
                    }
                } else if (mSurveyDataObject.getQuestionType()
                        .equalsIgnoreCase(AppConstant.ONE_WORD_ANSWER)
                        || mSurveyDataObject.getQuestionType()
                        .equalsIgnoreCase(
                                AppConstant.SHORT_DESCRIPTION_ANSWER)) {
                    if (!TextUtils
                            .isEmpty(mAnswerEditText.getText().toString())) {
                        // Escaping special characters to be saved in the DB:
                        mSurveyDataObject.setAnswer(mAnswerEditText.getText()
                                .toString().replace("'", "''"));
                        mAnswerEditText.setText(AppConstant.EMPTY_STRING);
                    }
                }
                mSurveyDataObject.setTimeZone(AppUtils.getTimeZoneShort());
                mSurveyDataObject.setTimeStamp(System.currentTimeMillis());
                mSurveyDataObject.setSessionId(PreferenceUtils.getSessionFromPreference(context));
                mSurveyDataObject.setSyncStatus(true);
                if (AppConstant.IsDeviceOnline) {
                    if (AppUtils.isLocationEnabledInDevice(context)) {
                        if ( PreferenceUtils.getDoubleFromPreference(context, AppConstant.LATITUDE) != 0 ) {
                            mSurveyDataObject.setCurrentLocation(
                                    PreferenceUtils.getStringFromPreference(context, AppConstant.CURRENT_LOCATION));
                            mSurveyDataObject.setLatitude(PreferenceUtils.getDoubleFromPreference(context, AppConstant.LATITUDE));
                            mSurveyDataObject.setLongitude(PreferenceUtils.getDoubleFromPreference(context, AppConstant.LONGITUDE));
                        }
                    }
                } else {
                    mSurveyDataObject.setOffline(true);
                }
                mResponseData.add(mSurveyDataObject);


                if (mQuestionCount <= mSurveyDataSize - 1) {
                    mOptionRadioGroup.removeAllViews();
                    inflateSurveyUI(context);
                } else {
                    loadSurveyData();
//                    DBManager.getInstance(context).saveSurveyData(mResponseData);
                    DBManager.getInstance(context).saveSurveyResponseToDB(mResponseData);
//                    if(AppUtils.isConnectedToInternet(context))
//                    DataGrinchNetworkTask.getInstance().sendSurveyResponse(context, mResponseData);
                    PreferenceUtils.saveBooleaninPreference(context, AppConstant.IS_SURVEY_DONE, true);
                    renderDialogs(DIALOG_THANK_YOU_SCREEN);
                }
            } else if (v.getId() == R.id.survey_dialog_close_text) {
                mDialog.dismiss();

            } else if (v.getId() == R.id.survey_skip_text_view) {
                try {
                    mAnswerEditText.setText("");                        //just avoid carrying the data from the one SDA to another
                } catch (Exception e) {

                }
                if (mQuestionCount <= mSurveyDataSize - 1) {
                    mOptionRadioGroup.removeAllViews();
                    inflateSurveyUI(context);
                } else {
                    renderDialogs(DIALOG_THANK_YOU_SCREEN);
//                    DBManager.getInstance(context).saveSurveyData(mResponseData);
                    DBManager.getInstance(context).saveSurveyResponseToDB(mResponseData);
//                    if(AppUtils.isConnectedToInternet(context))
//                    DataGrinchNetworkTask.getInstance().sendSurveyResponse(context, mResponseData);
                    PreferenceUtils.saveBooleaninPreference(context, AppConstant.IS_SURVEY_DONE, true);
                }
            } else if (v.getId() == R.id.ok_button) {
                mDialog.dismiss();
            }
        }
    };

    /**
     * Listener for button in the message dialogs.
     */
    private OnClickListener mMesssageDialogButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.ok_button) {
                mDialog.dismiss();
            } else if (v.getId() == R.id.cancel_button) {
                mDialog.dismiss();
            } else if (v.getId() == R.id.proceed_button) {
                renderDialogs(DIALOG_QUESTION_SCREEN);
            }
        }
    };

    /**
     * This method displays dialog.
     *
     * @param fragmentManager {@link android.support.v4.app.FragmentManager}
     * @param dialogId        Unique id for dialog.
     */
    public void show(FragmentManager fragmentManager, int dialogId) {
        mId = dialogId;
        String tag = String.valueOf(mId);

        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();
        Fragment prevFragment = fragmentManager.findFragmentByTag(tag);
        if (prevFragment != null) {
            fragmentTransaction.remove(prevFragment);
        }
        fragmentTransaction.commitAllowingStateLoss();

        show(fragmentManager, tag);
    }

    /**
     * on check changed listener for radio group to save selected response and
     * toggle the radio buttons
     */
    private OnCheckedChangeListener mRadioGroupListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (mPrevSelectedButton != null) {
                mPrevSelectedButton
                        .setCompoundDrawables(null, null, null, null);
            }
            RadioButton button = (RadioButton) group.getChildAt(checkedId);
            button.toggle();
            Drawable img = context.getResources().getDrawable(
                    R.drawable.tick);
            img.setBounds(0, 0, 20, 20);
            button.setCompoundDrawables(null, null, img, null);
            mPrevSelectedButton = button;
            mSurveyDataObject.setAnswer(button.getText().toString());
            mNextButton.setEnabled(true);
        }
    };


    /**
     * Method to set a custom drawable to the selected button as we need to change the theme as per the user.
     *
     * @return custom drawable for the view.
     */
    private StateListDrawable setCustomDrawable(SurveyDataTable surveyDataTable) {

        ShapeDrawable footerBackground = new ShapeDrawable();
        float[] radii = new float[8];
        radii[0] = 2;
        radii[1] = 2;
        radii[2] = 2;
        radii[3] = 2;
        footerBackground.setShape(new RoundRectShape(radii, null, null));
        int color = Color.parseColor(surveyDataTable.getBaseColor() != null ? surveyDataTable.getBaseColor() : "2980B9");
        footerBackground.getPaint().setColor(color);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_checked, android.R.attr.state_pressed}, context.getResources().getDrawable(R.drawable.normal_button_bg));
        states.addState(new int[]{android.R.attr.state_pressed}, footerBackground);
        states.addState(new int[]{android.R.attr.state_checked}, footerBackground);
        states.addState(new int[]{}, context.getResources().getDrawable(R.drawable.normal_button_bg));

        return states;
    }

    /**
     * Method to check if the answer edit box is empty or not.
     * If empty the user will not be allowed to move forward and vice versa.
     */
    void checkFieldsForEmptyValues() {

        String s1 = mAnswerEditText.getText().toString();

        if (s1.equals("")) {
            mNextButton.setEnabled(false);
        } else {
            mNextButton.setEnabled(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mParentFrameLayout = null;
        mSurveyDataList = null;
        mSurveyAnswerObject = null;
        mHeaderText = null;
        mCloseSurveyButton = null;
        mSkipText = null;
        mSurveyQuestion = null;
        mAnswerEditText = null;
        mOptionRadioGroup = null;
        mInflater = null;
        mSurveyProgressBar = null;
        mNextButton = null;
        mSurveyDataObject = null;
        mPrevSelectedButton = null;
        mDialog = null;

    }

}
