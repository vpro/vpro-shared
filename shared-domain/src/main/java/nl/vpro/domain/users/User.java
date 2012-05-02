/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 1 nov 2008.
 */
package nl.vpro.domain.users;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import nl.vpro.util.Helper;

/**
 * @author roekoe
 *
 */
public class User {

    private String principal;

    private String lastname;

    private String firstname;

    private String royalPrefix = "";

    private String email;

    private int version;

    /**
     * @return the principal
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * @param principal the principal to set
     */
    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    /**
     * @return the lastname
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * @param lastname the lastname to set
     */
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    /**
     * @return the firstname
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * @param firstname the firstname to set
     */
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    /**
     * @return the royalPrefix
     */
    public String getRoyalPrefix() {
        return royalPrefix;
    }

    /**
     * @param royalPrefix the royalPrefix to set
     */
    public void setRoyalPrefix(String royalPrefix) {
        this.royalPrefix = royalPrefix;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the version
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * @return The full name of the person [firstname] [royalPrefix] [lastname]
     */
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        Helper.appendIfNotEmpty(sb, getFirstname());
        sb.append(Helper.isEmpty(getFirstname()) ? "" : " ");
        Helper.appendIfNotEmpty(sb, getRoyalPrefix());
        sb.append(Helper.isEmpty(getRoyalPrefix()) ? "" : " ");
        Helper.appendIfNotEmpty(sb, getLastname());

        return sb.toString();
    }


    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 35).append(principal).toHashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User == false) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        User rhs = (User) obj;
        return new EqualsBuilder().append(principal, rhs.getPrincipal()).isEquals();
    }
}
