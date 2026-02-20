<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSLT template to transform a JSON-as-XML intermediate format back to
  PolicyCenter SOAP XML response. Used when the REST backend returns JSON
  that has been converted to an XML intermediate form and needs to be
  wrapped in a proper SOAP response.

  Issue #48 - Transformations
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:tns="http://guidewire.com/policycenter/ws">

    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

    <!-- Transform a generic policy JSON-XML to GetPolicyResponse -->
    <xsl:template match="/policy">
        <tns:GetPolicyResponse>
            <tns:policyNumber>
                <xsl:value-of select="policyNumber"/>
            </tns:policyNumber>
            <tns:policyType>
                <xsl:value-of select="policyType"/>
            </tns:policyType>
            <tns:status>
                <xsl:value-of select="status"/>
            </tns:status>
            <tns:effectiveDate>
                <xsl:value-of select="effectiveDate"/>
            </tns:effectiveDate>
            <tns:expirationDate>
                <xsl:value-of select="expirationDate"/>
            </tns:expirationDate>
            <tns:premium>
                <xsl:value-of select="premium"/>
            </tns:premium>
            <tns:customerId>
                <xsl:value-of select="customerId"/>
            </tns:customerId>
        </tns:GetPolicyResponse>
    </xsl:template>

    <!-- Transform a create response -->
    <xsl:template match="/createResult">
        <tns:CreatePolicyResponse>
            <tns:policyNumber>
                <xsl:value-of select="policyNumber"/>
            </tns:policyNumber>
            <tns:status>
                <xsl:value-of select="status"/>
            </tns:status>
            <tns:message>
                <xsl:value-of select="message"/>
            </tns:message>
        </tns:CreatePolicyResponse>
    </xsl:template>

    <!-- Transform a list of policies -->
    <xsl:template match="/policies">
        <tns:ListPoliciesResponse>
            <xsl:for-each select="item">
                <tns:policy>
                    <tns:policyNumber>
                        <xsl:value-of select="policyNumber"/>
                    </tns:policyNumber>
                    <tns:policyType>
                        <xsl:value-of select="policyType"/>
                    </tns:policyType>
                    <tns:status>
                        <xsl:value-of select="status"/>
                    </tns:status>
                    <tns:premium>
                        <xsl:value-of select="premium"/>
                    </tns:premium>
                </tns:policy>
            </xsl:for-each>
        </tns:ListPoliciesResponse>
    </xsl:template>

    <!-- Default: suppress unmatched text -->
    <xsl:template match="text()"/>
</xsl:stylesheet>
