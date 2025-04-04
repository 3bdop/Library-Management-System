package main.java.model;

public class Member {
        private int memberId;
        private String name;
        private String phone;
        private String registrationDate;

    public Member(int memberId, String name, String phone, String registrationDate) {
        this.memberId = memberId;
        this.name = name;
        this.phone = phone;
        this.registrationDate = registrationDate;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }
}
