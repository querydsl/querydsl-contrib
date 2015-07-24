package com.querydsl.elasticsearch.domain;

import com.google.common.base.Objects;
import com.querydsl.core.annotations.QuerySupertype;

@QuerySupertype
public abstract class AbstractEntity {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return id != null ? 0 : id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof AbstractEntity) {
            AbstractEntity other = (AbstractEntity) obj;
            return Objects.equal(other.getId(), id);
        } else {
            return false;
        }
    }


}
