package mlakir.aura.core.services.tabiturient;

public final class TabiturientSelectors {

    public static final String RESULT_CONTAINER = "#resultsliv";
    public static final String REVIEW_CARD = ".mobpadd20-2 > .p20[class*=request]";
    public static final String REVIEW_TEXT = "div[style*=text-align:justify].font2";
    public static final String REVIEW_TEXT_EXPAND_BUTTON = "b[class^=slivmore-pre-]";
    public static final String REVIEW_TEXT_HIDDEN_PART = "span[class^=slivmore-post-]";
    public static final String REVIEW_ID_FROM_DOVERIE = "table.doverieline[id^=doverieform]";
    public static final String REVIEW_ID_FROM_LIKE = "table.like[id^=likediv]";
    public static final String REVIEW_ORIGINAL_URL = "a[href*=/sliv/n/?]";
    public static final String REVIEW_AUTHOR = ".avatarask2 + td span.font2 b";
    public static final String REVIEW_DATE = ".dateinrat + td span.font2";
    public static final String REVIEW_FACULTY = "table.tag b.font11";

    private TabiturientSelectors() {
    }
}
