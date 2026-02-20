<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSLT template to transform PolicyCenter SOAP XML to a JSON-like XML
  intermediate format. Used as part of the SOAP-to-REST transformation
  pipeline when XSLT-based transformations are preferred over programmatic ones.

  Issue #48 - Transformations
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:pc="http://guidewire.com/policycenter/ws"
                exclude-result-prefixes="pc">

    <xsl:output method="text" encoding="UTF-8"/>

    <!-- Transform GetPolicyRequest to JSON -->
    <xsl:template match="pc:GetPolicyRequest">
        <xsl:text>{"policyNumber":"</xsl:text>
        <xsl:value-of select="pc:policyNumber"/>
        <xsl:text>"}</xsl:text>
    </xsl:template>

    <!-- Transform CreatePolicyRequest to JSON -->
    <xsl:template match="pc:CreatePolicyRequest">
        <xsl:text>{</xsl:text>
        <xsl:text>"policyType":"</xsl:text>
        <xsl:value-of select="pc:policyType"/>
        <xsl:text>","customerId":"</xsl:text>
        <xsl:value-of select="pc:customerId"/>
        <xsl:text>","effectiveDate":"</xsl:text>
        <xsl:value-of select="pc:effectiveDate"/>
        <xsl:text>","expirationDate":"</xsl:text>
        <xsl:value-of select="pc:expirationDate"/>
        <xsl:text>","premium":</xsl:text>
        <xsl:value-of select="pc:premium"/>
        <xsl:text>}</xsl:text>
    </xsl:template>

    <!-- Transform ListPoliciesRequest to JSON -->
    <xsl:template match="pc:ListPoliciesRequest">
        <xsl:text>{</xsl:text>
        <xsl:if test="pc:customerId">
            <xsl:text>"customerId":"</xsl:text>
            <xsl:value-of select="pc:customerId"/>
            <xsl:text>"</xsl:text>
        </xsl:if>
        <xsl:text>}</xsl:text>
    </xsl:template>

    <!-- Default: suppress unmatched text -->
    <xsl:template match="text()"/>
</xsl:stylesheet>
