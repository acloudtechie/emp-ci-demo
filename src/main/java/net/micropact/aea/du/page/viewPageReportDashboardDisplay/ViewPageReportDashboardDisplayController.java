package net.micropact.aea.du.page.viewPageReportDashboardDisplay;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;
import net.micropact.aea.utility.data.ResultGroup;

/**
 * This is the controller code for a page which will list all Pages and Reports that Users have displayed on their
 * dashboards. This is important for two reasons. The main reason is that often time users will put extremely
 * large pages or reports on their dashboard which lock up or slow down the system. The second is that sometimes
 * users will complain that they do not see something on their dashboard which they should. This page can be used
 * to check that a particular user has the necessary pages on their dashboards.
 *
 * @author zmiller
 */
public class ViewPageReportDashboardDisplayController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {

        final TextResponse response = etk.createTextResponse();

        final List<ItemGroup> itemGroups = new LinkedList<>();

        itemGroups.add(buildItemGroup(Type.REPORT, "report.manager.ReportManager.do?method=open&id=", etk.createSQL("SELECT savedReport.saved_report_id RESULT_ID, savedReport.name RESULT_NAME, u.user_id USER_ID, u.username USERNAME FROM etk_saved_report savedReport JOIN etk_report_dashboard_option reportDashboardOption ON reportDashboardOption.saved_report_id = savedReport.saved_report_id JOIN etk_user u ON u.user_id = reportDashboardOption.user_id WHERE reportDashboardOption.display_on_dashboard = 1 ORDER BY savedReport.name, savedReport.saved_report_id, u.username, u.user_id")
                .fetchList()));

        itemGroups.add(buildItemGroup(Type.PAGE, "page.update.request.do?id=", etk.createSQL("SELECT page.page_id RESULT_ID, page.name RESULT_NAME, u.user_id USER_ID, u.username USERNAME FROM etk_page page JOIN etk_page_dashboard_option pageDashboardOption ON pageDashboardOption.page_id = page.page_id JOIN etk_user u ON u.user_id = pageDashboardOption.user_id WHERE pageDashboardOption.display_on_dashboard = 1 ORDER BY page.name, page.page_id, u.username, u.user_id")
                .fetchList()));

        response.put("itemGroups", toJson(itemGroups));

        return response;
    }

    /**
     * Converts a list of item groups to a JSON representation.
     *
     * @param itemGroups List of Item Groups
     * @return A JSON representation of the list of Item Groups
     */
    private static String toJson(final List<ItemGroup> itemGroups){
        final List<Map<String, Object>> itemGroupList = new LinkedList<>();

        for(final ItemGroup itemGroup : itemGroups){
            final List<Object> itemsList = new LinkedList<>();
            for(final Item item : itemGroup.getItems()){
                final List<Map<String, Object>> usersList = new LinkedList<>();

                for(final User user : item.getUsers()){
                    final Map<String, Object> userMap = Utility.arrayToMap(String.class, Object.class, new Object[][]{
                        {"userId", user.getUserId()},
                        {"name", user.getName()}
                    });
                    usersList.add(userMap);
                }

                final Map<String, Object> itemMap = Utility.arrayToMap(String.class, Object.class, new Object[][]{
                    {"name", item.getName()},
                    {"url", item.getUrl()},
                    {"users", usersList}
                });

                itemsList.add(itemMap);
            }

            final Map<String, Object> itemGroupMap = Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"type", itemGroup.getType().getName()},
                {"items", itemsList}});

            itemGroupList.add(itemGroupMap);
        }

        return JsonUtilities.encode(itemGroupList);
    }

    /**
     * Converts a result which has the form of one returned by etk.createSQL().fetchList() into an ItemGroup
     * which is statically typed and easier to work with downstream. The URL for the items in the group
     * will be a concatenation of urlPrefix and the value of RESULT_ID key in objects.
     *
     * @param itemType The Type of the ItemGroup
     * @param urlPrefix The prefix for the URLs
     * @param objects a map representation of the items in the group. It must have the following keys:
     *          <ul>
     *              <li>RESULT_ID</li>
     *              <li>RESULT_NAME</li>
     *              <li>USERNAME</li>
     *              <li>USER_ID</li>
     *          </ul>
     * @return A representation of objects.
     */
    private ItemGroup buildItemGroup(final Type itemType,
            final String urlPrefix,
            final List<Map<String, Object>> objects){
        final List<ResultGroup<Number>> pageGroups = ResultGroup.buildResultGroups("RESULT_ID",
                objects,
                Number.class);

        final List<Item> pages = new LinkedList<>();

        for(final ResultGroup<Number> pageGroup : pageGroups){

            final List<User> users = new LinkedList<>();

            for(final Map<String, Object> userMap : pageGroup.getResults()){
                users.add(new User((String) userMap.get("USERNAME"), ((Number) userMap.get("USER_ID")).longValue()));
            }

            pages.add(new Item((String) pageGroup.getResults().get(0).get("RESULT_NAME"),
                    urlPrefix + pageGroup.getResults().get(0).get("RESULT_ID"),
                    users));
        }

        return new ItemGroup(itemType, pages);
    }

    /**
     * Represents a group of Items that users have chosen to display on their dashboard.
     * As of this writing there will be two Item Groups. One for Pages and another for Reports.
     *
     * @author zmiller
     */
    public final class ItemGroup{
        private final Type type;
        private final List<Item> items;

        /**
         * Construct an item group given the type of items and the items themselves.
         *
         * @param theType Type of items within the group
         * @param theItems The Items which are part of the group
         */
        public ItemGroup(final Type theType, final List<Item> theItems){
            type = theType;
            items = theItems;
        }

        /**
         * Get the type of the items held by the item group.
         *
         * @return The Type of the Items in the group
         */
        public Type getType(){
            return type;
        }

        /**
         * Get the items within this group.
         *
         * @return The Items in the group
         */
        public List<Item> getItems(){
            return items;
        }
    }

    /**
     * An item represents an entity which has been placed on a user's dashboard.
     * For example a specific Page or specific Report is an Item.
     *
     * @author zmiller
     */
    public final class Item{
        private final String name;
        private final String url;
        private final List<User> users;

        /**
         * Construct a new item given its name, URL and the users who have it listed.
         *
         * @param theName Name of the item to be displayed to the end user
         * @param theUrl URL which can be used to access the item within entellitrak
         * @param theUsers The users who have placed this Item on their dashboard
         */
        Item(final String theName, final String theUrl, final List<User> theUsers){
            name = theName;
            url = theUrl;
            users = theUsers;
        }

        /**
         * Get the name of the item.
         *
         * @return Name of the item displayed to the end user
         */
        public String getName(){
            return name;
        }

        /**
         * Get the URL the item is located at.
         *
         * @return URL which can be used to access the item within entellitrak
         */
        public String getUrl(){
            return url;
        }

        /**
         * Get the users who have the item listed on their dashboard.
         *
         * @return Users who have this Item displayed on their dashboard
         */
        public List<User> getUsers(){
            return users;
        }
    }

    /**
     * Represents an entellitrak user.
     *
     * @author zmiller
     */
    public final class User{

        private final String username;
        private final long userId;

        /**
         * Get Construct a user given their username and user id.
         *
         * @param theUsername THe username of the user
         * @param theUserId The user id of the user
         */
        User(final String theUsername, final long theUserId){
            username = theUsername;
            userId = theUserId;
        }

        /**
         * Get the username of the user.
         *
         * @return The username of the user
         */
        public String getName(){
            return username;
        }

        /**
         * Get the user id of the user.
         *
         * @return The user id of the user
         */
        public long getUserId(){
            return userId;
        }
    }

    /**
     * Represents the Type of Item which can be placed on the dashboard of a user.
     *
     * @author zmiller
     */
    public enum Type{

        /**
         * Represents entellitrak custom pages.
         */
        PAGE("Page"),
        /**
         * Represents entellitrak custom reports.
         */
        REPORT("Report");

        private final String name;

        /**
         * Construct a new Type.
         *
         * @param theName A user readable representation of the Type.
         */
        Type(final String theName){
            name = theName;
        }

        /**
         * Get the user-readable representation of the Type.
         *
         * @return A user-readable representation of the Type.
         */
        public String getName(){
            return name;
        }
    }
}
