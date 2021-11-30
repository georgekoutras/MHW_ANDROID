package gr.openit.smarthealthwatch;

public class InvitationRow {

    String sender;
    String id;
    String relation;
    Boolean seen;
    boolean incoming;
    public InvitationRow(String id,String sender, String relation, Boolean seen,Boolean incoming) {
        this.id = id;
        this.sender = sender;
        this.relation = relation;
        this.seen = seen;
        this.incoming = incoming;
    }

    public String getSender() {
        return sender;
    }

    public String getRelation() { return relation; }

    public Boolean getSeen() { return seen; }

    public String getId() {
        return id;
    }

    public Boolean isIncoming() { return incoming; }
}
