package ca.uwaterloo.crysp.privacyguard.Application.Database;

/**
 * Created by MAK on 03/11/2015.
 */

public class CategorySummary {
    public int notifyId;
    public String category;
    public int count;
    public int ignore;

    public CategorySummary(int notifyId, String category, int count, int ignore){
        this.notifyId = notifyId;
        this.category = category;
        this.count = count;
        this.ignore = ignore;
    }
}
