package org.cardanofoundation.metadatatools.api.model.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.cardanofoundation.metadatatools.api.model.rest.TokenMetadata;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import java.util.Date;

@Log4j2
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "metadata")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class SubmitToken {

    @Id
    private String subject;
    @Transient
    @JsonIgnore
    private String assetName;

    @Column(name = "name")
    private String name;
    @Column(name = "description" , columnDefinition="TEXT")

    private String description;
    @Column(name = "ticker")

    private String ticker;
    @Column(name = "logo" , columnDefinition="TEXT")
    private String logo;
    @Column(name = "url" , columnDefinition="TEXT")
    private String url;
    @Column(name = "policy" ,columnDefinition="TEXT")
    private String policy;
    @Column(name = "decimals")
    private Integer decimals;
    @Column(name = "updated")
    private Date updated;
    @Column(name = "updated_by" ,columnDefinition="TEXT")

    private String updatedBy;
    @Column(name = "status" )
    private String status;
    @Column(name = "reject_url")
    private String rejectUrl;
    @JsonIgnore
    @Type(type = "jsonb")
    @Column(name = "properties" , columnDefinition = "jsonb")
    private TokenMetadata properties;
    @Transient
    @JsonIgnore
    private MultipartFile policyScript;
    @Transient
    @JsonIgnore
    private MultipartFile policySkey;


    public SubmitToken(String subject) {
        this.subject = subject;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getDecimals() {
        return decimals;
    }

    public void setDecimals(Integer decimals) {
        this.decimals = decimals;
    }

    public MultipartFile getPolicyScript() {
        return policyScript;
    }

    public void setPolicyScript(MultipartFile policyScript) {
        this.policyScript = policyScript;
    }

    public MultipartFile getPolicySkey() {
        return policySkey;
    }

    public void setPolicySkey(MultipartFile policySkey) {
        this.policySkey = policySkey;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public TokenMetadata getProperties() {
        return properties;
    }

    public void setProperties(TokenMetadata properties) {
        this.properties = properties;
    }

    public String getRejectUrl() {
        return rejectUrl;
    }

    public void setRejectUrl(String rejectUrl) {
        this.rejectUrl = rejectUrl;
    }
}
