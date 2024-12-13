package com.crm.rdvision.common;

import com.crm.rdvision.utility.Action;

public final class EndPointReference {
    //user
    public static final String CREATE_USER = "createUser";
    public static final String DELETE_USER = "deleteUser/{userId}";

    //product
    public static final String CREATE_PRODUCT = "createProduct";
    public static final String GET_ALL_PRODUCT = "getAllProducts";
    public static final String GET_PRODUCT = "getProduct/{productId}";



    public static final String UPDATE_USER = Action.UPDATE;
    public static final String GET_USER = Action.GET+"/{userId}";
    public static final String GET_ALL_USERS = "getAllUsers";

    public static final String CREATE_DEPARTMENT = "createDepartment";
    public static final String GET_DEPARTMENT = "getDepartment/{deptId}";
    public static final String GET_ALL_DEPARTMENT = "getDepartments";
    public static final String GET_ALL_TEAM = "getAllTeams";
    public static final String CREATE_ROLE = "createRole";
    public static final String GET_ROLE = "getRole/{roleId}";
    public static final String GET_ALL_ROLES = "getAllRoles";
    public static final String CREATE_TEAM = "createTeam";
    public static final String GET_TEAM = "getTeam/{teamId}";
    public static final String ASSIGN_TICKT_TO_TAEM = "assignToTeam/{teamId}";
    public static final String ASSIGN_TICKT_TO_USER = "assignToUser/{userId}";
    public static final String GET_ALL_TICKET_BY_TEAM = "getTicketsOfTeam/{teamId}/{ticketStatus}";
    public static final String GET_ALL_TICKET_BY_STATUS = "ticketByStatus";
    public static final String ADD_TO_ORDER = "addToOrder";
    public static final String GET_ORDER = "getOrder/{ticketId}";
    public static final String GET_TICKET = "getTicket/{ticketId}";
    public static final String UPDATE_TICKET_RESPONSE = "updateTicketResponse/{ticketId}";
    public static final String CREATE_INVICE = "createInvice";
    public static final String CREATE_ADDRESS = "createAddress";
    public static final String GET_ADDRESS = "getAddress/{ticketId}";
    public static final String GET_ALL_ORDER = "getAllOrder";


    //    Uploade ticket
    public static  final String Upload_ticket = "addCsvTicket";
    public static final String Get_uploaded_tickets_By_Status="allTicketsByStatus";
    //Upload CSV Products
    public  static final String Upload_CSV_Products ="uploadproductscsv";

    //Live Tickets
    public static final String Get_All_Tickets_Assign_To_User="getAllByUser/{userId}";
    public static final String Count_Of_Followups_Of_User ="followUpByDate/{userId}";
    public static final String Ratio_Of_Tickets="getRatioOfTickets";
    public static final String Click_To_Call_By_Mobile_Number="clickToCall";
    public static final String No_Of_Today_Followup_Of_User="todayfollowup/{userId}";
    public static final String Negotiation_Based_Tickets="negotiationstagebased";
    public static final String Click_To_Call_By_Mobile_Ticket="clickToCall/{ticketId}";



    //Stripe Webhook
    public static final String Stripe_webhook_Call="/webhook";

}