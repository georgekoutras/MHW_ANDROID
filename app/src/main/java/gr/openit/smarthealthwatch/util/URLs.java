package gr.openit.smarthealthwatch.util;

public class URLs {
    //private static final String ROOT_URL = "http://sh1.openit.gr:5002/api/";
    private static final String ROOT_URL = "https://app.myhealthwatcher-project.gr/api/";

    public static final String URL_REGISTER = ROOT_URL + "PrimaryUser";
    public static final String URL_LOGIN= ROOT_URL + "PrimaryUser/";
    public static final String URL_ACCESS_TOKEN= ROOT_URL +"token";
    public static final String URL_ADD_MEASUREMENT= ROOT_URL + "PrimaryUser/{id}/Measurements";
    public static final String URL_GET_PRIMARY_USER_INFO = ROOT_URL + "PrimaryUser/{id}";
    public static final String URL_UPDATE_PRIMARY_USER_INFO = ROOT_URL + "PrimaryUser/{id}";
    public static final String URL_GET_MEASUREMENT = ROOT_URL + "PrimaryUser/{id}/Measurements";
    public static final String URL_DELETE_MEASUREMENT = ROOT_URL + "PrimaryUser/{id}/Measurements/{mes_id}";
    public static final String URL_GET_THRESHOLDS = ROOT_URL + "PrimaryUser/{id}/Measurements/Thresholds";
    public static final String URL_GET_ADVICES = ROOT_URL + "PrimaryUser/{id}/Tips";
    public static final String URL_GET_ADVICE_DETAILS = ROOT_URL + "PrimaryUser/Tips/{id}";
    public static final String URL_GET_MESSAGES = ROOT_URL + "PrimaryUser/{id}/Messages";
    public static final String URL_DELETE_MESSAGE = ROOT_URL + "PrimaryUser/{id}/Messages/{message_id}";
    public static final String URL_MARKASREAD_MESSAGE = ROOT_URL + "PrimaryUser/{id}/Messages/{message_id}";
    public static final String URL_GET_ALERTS = ROOT_URL + "PrimaryUser/{id}/Alerts";
    public static final String URL_MARKASREAD_ALERT = ROOT_URL + "PrimaryUser/{id}/Alerts/{alert_id}";
    public static final String URL_GET_CONTACTS = ROOT_URL + "PrimaryUser/{id}/Contacts";
    public static final String URL_GET_CONTACT_DETAILS = ROOT_URL + "PrimaryUser/{id}/Contacts/{contact_id}";
    public static final String URL_UPDATE_CONTACT = ROOT_URL + "PrimaryUser/{id}/Contacts/{contact_id}";
    public static final String URL_DELETE_CONTACT = ROOT_URL + "PrimaryUser/{id}/Contacts/{contact_id}";
    public static final String URL_GET_INVITATIONS = ROOT_URL + "PrimaryUser/{id}/Invitations";
    public static final String URL_GET_INVITATION_DETAILS = ROOT_URL + "PrimaryUser/{id}/Invitations/{invitation_id}";
    public static final String URL_ACCEPT_REJECT_INVITATION = ROOT_URL + "PrimaryUser/{id}/Invitations/{invitation_id}";
    public static final String URL_DELETE_INVITATION = ROOT_URL + "PrimaryUser/{id}/Invitations/{invitation_id}";
    public static final String URL_MARKASREAD_INVITATION = ROOT_URL + "PrimaryUser/{id}/Invitations/{invitation_id}";
    public static final String URL_SEND_INVITATION = ROOT_URL + "PrimaryUser/{id}/Invitations";
    public static final String URL_RESET_PASSWORD = ROOT_URL + "PrimaryUser/PasswordReset";
    public static final String MEASUREMENT_TYPE = "measurementType=";
    public static final String START_TIME = "startTime=";
    public static final String END_TIME = "endTime=";
}
