package org.eclipse.kura.core.keystore.request.handler.test;

import static org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants.ARGS_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.core.keystore.request.handler.KeystoreServiceRequestHandlerV1;
import org.eclipse.kura.core.keystore.util.CertificateInfo;
import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.core.keystore.util.EntryType;
import org.eclipse.kura.core.keystore.util.PrivateKeyInfo;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class KeystoreServiceRequestHandlerTest {

    private final String EMPTY_KEYSTORE_1 = "[{\"id\":\"MyKeystore\",\"type\":\"jks\",\"size\":0}]";
    private final String EMPTY_KEYSTORE_2 = "[{\"id\":\"MyKeystore\",\"type\":\"pkcs12\",\"size\":0}]";
    private final String KEYSTORE_ENTRY_1 = "[{\"subjectDN\":\"CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown\",\"issuer\":\"CN=Unknown,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown\",\"startDate\":\"Apr 14, 2021 10:02:28 AM\",\"expirationdate\":\"Jul 13, 2021 10:02:28 AM\",\"algorithm\":\"SHA256withRSA\",\"id\":\"MyKeystore:alias\",\"keystoreName\":\"MyKeystore\",\"alias\":\"alias\",\"type\":\"TRUSTED_CERTIFICATE\"}]";
    private final String KEYSTORE_ENTRY_2 = "[{\"subjectDN\":\"CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown\",\"issuer\":\"CN=Unknown,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown\",\"startDate\":\"Apr 14, 2021, 10:02:28 AM\",\"expirationdate\":\"Jul 13, 2021, 10:02:28 AM\",\"algorithm\":\"SHA256withRSA\",\"id\":\"MyKeystore:alias\",\"keystoreName\":\"MyKeystore\",\"alias\":\"alias\",\"type\":\"TRUSTED_CERTIFICATE\"}]";
    private final String KEYSTORE_ENTRY_WITH_CERT_1 = "{\"subjectDN\":\"CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown\",\"issuer\":\"CN=Unknown,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown\",\"startDate\":\"Apr 14, 2021 10:02:28 AM\",\"expirationdate\":\"Jul 13, 2021 10:02:28 AM\",\"algorithm\":\"SHA256withRSA\",\"certificate\":\"-----BEGIN CERTIFICATE-----\\nMIIDdzCCAl+gAwIBAgIEQsO0gDANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV\\nbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\\nVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du\\nMB4XDTIxMDQxNDA4MDIyOFoXDTIxMDcxMzA4MDIyOFowbDEQMA4GA1UEBhMHVW5r\\nbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE\\nChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC\\nASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJSWJDxu8UNC4JGOgK31WCvz\\nNKy2ONH+jTVKnBY7Ckb1hljJY0sKO55aG1HNDfkev2lJTsPIz0nJjNsqBvB1flvf\\nr6XVCxdN0yxvU5g9SpRxE/iiPX0Qt7463OfzyKW97haJrrhF005RHYNcORMY/Phj\\nhFDnZhtAwpbQLzq2UuIZ7okJsx0IgRbjH71ZZuvYCqG7Ct/bp1D7w3tT7gTbIKYH\\nppQyG9rJDEh9+cr9Hyk8Gz7aAbPT/wMH+/vXDjH2j/M1Tmed0ajuGCJumaTQ4eHs\\n9xW3B3ugycb6e7Osl/4ESRO5RQL1k2GBONv10OrKDoZ5b66xwSJmC/w3BRWQ1cMC\\nAwEAAaMhMB8wHQYDVR0OBBYEFPospETb5HNeD/DmS9mwt+v/AYq/MA0GCSqGSIb3\\nDQEBCwUAA4IBAQBxMe1xQVQKt36A5qVlEZyxI9eb6eQRlYzorOgP2tFaOsvDPpRI\\nCALhPmxgQl/5QvKFfCXKoxWj1Spg4sF6fJp6jhSjLpmChS9lf5fRaWS20/pxIddM\\n10diq3r6HxLKSxCYK7Pf5scOeZquvwfo8Kxye01bvCMFf1s1K3ZEZszk5Oo2MnWU\\nU22YnXfZm1C0h2WMUcou35A7CeVAHPWI0Rvefojv1qYlQScJOkCN5lO6C/1qvRhq\\nnDQdQN/m1HQbpfh2DD6F33nBjkyLQyMRF8uMnspLrLLj8lecSTJZO4fGJOaIXh3O\\n44da9A02FAf5nRRQpwP2x/4IZ5RTRBzrqbqD\\n-----END CERTIFICATE-----\",\"id\":\"MyKeystore:alias\",\"keystoreName\":\"MyKeystore\",\"alias\":\"alias\",\"type\":\"TRUSTED_CERTIFICATE\"}";
    private final String KEYSTORE_ENTRY_WITH_CERT_2 = "{\"subjectDN\":\"CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown\",\"issuer\":\"CN=Unknown,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown\",\"startDate\":\"Apr 14, 2021, 10:02:28 AM\",\"expirationdate\":\"Jul 13, 2021, 10:02:28 AM\",\"algorithm\":\"SHA256withRSA\",\"certificate\":\"-----BEGIN CERTIFICATE-----\\nMIIDdzCCAl+gAwIBAgIEQsO0gDANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV\\nbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\\nVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du\\nMB4XDTIxMDQxNDA4MDIyOFoXDTIxMDcxMzA4MDIyOFowbDEQMA4GA1UEBhMHVW5r\\nbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE\\nChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC\\nASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJSWJDxu8UNC4JGOgK31WCvz\\nNKy2ONH+jTVKnBY7Ckb1hljJY0sKO55aG1HNDfkev2lJTsPIz0nJjNsqBvB1flvf\\nr6XVCxdN0yxvU5g9SpRxE/iiPX0Qt7463OfzyKW97haJrrhF005RHYNcORMY/Phj\\nhFDnZhtAwpbQLzq2UuIZ7okJsx0IgRbjH71ZZuvYCqG7Ct/bp1D7w3tT7gTbIKYH\\nppQyG9rJDEh9+cr9Hyk8Gz7aAbPT/wMH+/vXDjH2j/M1Tmed0ajuGCJumaTQ4eHs\\n9xW3B3ugycb6e7Osl/4ESRO5RQL1k2GBONv10OrKDoZ5b66xwSJmC/w3BRWQ1cMC\\nAwEAAaMhMB8wHQYDVR0OBBYEFPospETb5HNeD/DmS9mwt+v/AYq/MA0GCSqGSIb3\\nDQEBCwUAA4IBAQBxMe1xQVQKt36A5qVlEZyxI9eb6eQRlYzorOgP2tFaOsvDPpRI\\nCALhPmxgQl/5QvKFfCXKoxWj1Spg4sF6fJp6jhSjLpmChS9lf5fRaWS20/pxIddM\\n10diq3r6HxLKSxCYK7Pf5scOeZquvwfo8Kxye01bvCMFf1s1K3ZEZszk5Oo2MnWU\\nU22YnXfZm1C0h2WMUcou35A7CeVAHPWI0Rvefojv1qYlQScJOkCN5lO6C/1qvRhq\\nnDQdQN/m1HQbpfh2DD6F33nBjkyLQyMRF8uMnspLrLLj8lecSTJZO4fGJOaIXh3O\\n44da9A02FAf5nRRQpwP2x/4IZ5RTRBzrqbqD\\n-----END CERTIFICATE-----\",\"id\":\"MyKeystore:alias\",\"keystoreName\":\"MyKeystore\",\"alias\":\"alias\",\"type\":\"TRUSTED_CERTIFICATE\"}";
    private final String CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDdzCCAl+gAwIBAgIEQsO0gDANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV\n"
            + "bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\n"
            + "VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du\n"
            + "MB4XDTIxMDQxNDA4MDIyOFoXDTIxMDcxMzA4MDIyOFowbDEQMA4GA1UEBhMHVW5r\n"
            + "bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE\n"
            + "ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC\n"
            + "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJSWJDxu8UNC4JGOgK31WCvz\n"
            + "NKy2ONH+jTVKnBY7Ckb1hljJY0sKO55aG1HNDfkev2lJTsPIz0nJjNsqBvB1flvf\n"
            + "r6XVCxdN0yxvU5g9SpRxE/iiPX0Qt7463OfzyKW97haJrrhF005RHYNcORMY/Phj\n"
            + "hFDnZhtAwpbQLzq2UuIZ7okJsx0IgRbjH71ZZuvYCqG7Ct/bp1D7w3tT7gTbIKYH\n"
            + "ppQyG9rJDEh9+cr9Hyk8Gz7aAbPT/wMH+/vXDjH2j/M1Tmed0ajuGCJumaTQ4eHs\n"
            + "9xW3B3ugycb6e7Osl/4ESRO5RQL1k2GBONv10OrKDoZ5b66xwSJmC/w3BRWQ1cMC\n"
            + "AwEAAaMhMB8wHQYDVR0OBBYEFPospETb5HNeD/DmS9mwt+v/AYq/MA0GCSqGSIb3\n"
            + "DQEBCwUAA4IBAQBxMe1xQVQKt36A5qVlEZyxI9eb6eQRlYzorOgP2tFaOsvDPpRI\n"
            + "CALhPmxgQl/5QvKFfCXKoxWj1Spg4sF6fJp6jhSjLpmChS9lf5fRaWS20/pxIddM\n"
            + "10diq3r6HxLKSxCYK7Pf5scOeZquvwfo8Kxye01bvCMFf1s1K3ZEZszk5Oo2MnWU\n"
            + "U22YnXfZm1C0h2WMUcou35A7CeVAHPWI0Rvefojv1qYlQScJOkCN5lO6C/1qvRhq\n"
            + "nDQdQN/m1HQbpfh2DD6F33nBjkyLQyMRF8uMnspLrLLj8lecSTJZO4fGJOaIXh3O\n"
            + "44da9A02FAf5nRRQpwP2x/4IZ5RTRBzrqbqD\n" + "-----END CERTIFICATE-----";
    private final String JSON_MESSAGE_PUT_CERT = "{\n" + "   \"keystoreName\":\"MyKeystore\",\n"
            + "   \"alias\":\"myCertTest99\",\n" + "   \"type\":\"TrustedCertificate\",\n" + "   \"certificate\":\""
            + CERTIFICATE + "\n" + "}";
    private final String JSON_MESSAGE_DEL = "{\n" + "    \"keystoreName\" : \"MyKeystore\",\n"
            + "    \"alias\" : \"mycerttestec\"\n" + "}";
    private final String PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n"
            + "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCCcZ1Nu9AVYKJd\n"
            + "p6gcdObxCiofeOVbJv3Ws19JVa6PGTSgREFy5c97/k+SsSBhHAFp1n3738E2gdxD\n"
            + "auftKpmV3ZZ93rEQQ+Db71PiyFlrEEtcDE//14EH2jaHBIghxqWmgvWu0e8pca4u\n"
            + "xnmOOJAPCabNYLLs4pnTh9xJn+B+Mdz+/NNj/C7BV53W2nAcsdVqpbmLjfCrDSd/\n"
            + "hgel8AoAbdjiRGkYHkgvEuztjx01pO2iGAgpkctigdxF/ygwwOOxcPASw/55ZjSE\n"
            + "gMZx7PMyxEiIL7jgt/cgG68QhlQ3neYfJ9cd+gLvn1g1fwsGtGpw3Mh3dgs6DQkr\n"
            + "8HMQW0bpAgMBAAECggEAL2IR3/C/L2TA1gBWwq98TEaC8pe5yJirUFgr3rmvBPAE\n"
            + "+8qPc6si6UmBoimRN3Uy1j1B2kJ3LtORLTQiNzZoP9YUGnjQHLZrcbjH4fMg+BEd\n"
            + "LrySOr8PccjEUdtFj+9WsNuVXwGHPKi8uuUBtrW5Lp006BmeJQpTElGhpWTb6Tqy\n"
            + "OcNceNz+oP1N3AZ3Mnf6Aq6GuvD4QeGMVEiosHPxMqY0eddNK672zq3A0o1NPA9z\n"
            + "yaVt9UK7SZ6/yOKYrAAM/2iLHmNbrYRer567hgg2B1LGJCRSdB/c34u6lTnPo+Ai\n"
            + "olQahQNFhiJKTXr2kK9WgS6tWXhqUqsVLUCug4mDAQKBgQDKLbhidAYClyOKJW7U\n"
            + "GsbM5dmQV80NsnsN3qphKZDTEMinhs3N4oknhi+mZPeyge4MY73BiHXhnMcUkfhw\n"
            + "nbhRNhrkZTt18S6rTzEp/vDDx+ZosxRKYXmPyuDDWDmvG6ocRyOLIgoYhT0kPQ7a\n"
            + "oXQkpFPjBq1UmqNOcUwEpNG2sQKBgQClKzyAopsjWNptBQPo/j/PN24uThpdd3yX\n"
            + "NenmLZsYJyloKDbOGzEXpuyzdtNAiIVDsQ8RzN5lkF6xVvXnOlSA2gmEc8KJmRl8\n"
            + "/gWzPRHHHNR7QUiGmg9QThrUp2l6DiAm/IcuL0btj99kQa2XcLGlTohwWpdVySSx\n"
            + "abDX7pSRuQKBgH/jy+77VZHt6R1J8IFbLsYN30HfSGaRsCVl5IDxuhrJUyQlsam6\n"
            + "0uediibHV6gjaGGN9kql92tvsL7iVzVlj2JPx1MSdjp1BgB3Z7IZAlPV73nrTbp/\n"
            + "TlYXD3aCKHsMFN8uYN1x+tDn93Uk6nCCEOXczPOfFaWe7A6CvINzfvUBAoGAUJEm\n"
            + "khi/VB6jbUpk/eIHfiyrsiqm8bC3NYs27PCSFtYDfKshEKhy6faiv2fW5EOzvbFA\n"
            + "iI5GbYRerGKe0IvDbJbuzY0p97SWmkHOxf+kDFwjyXuuxPmhPqraq6B98uuxA1Nr\n"
            + "HTwyfO8RKPZglt6ByQDlzOhjqZTUMTY87ReToQECgYBKdX3Idr4zvODkXIlG852C\n"
            + "o135W+7AWr+dYRLx1FcvgMU9SbF9cwUU5Zutbrv+Kl8xGPyfx09MJ6BNxTkkr09J\n"
            + "BpbrbOZsUDjMjojyQYL4Ll9rLohk+Pq73xXJjtTRIXZVXJg27pEEqzcVB4o9vgli\n" + "yzOqhyTKM9JP7Uda6Fv6DA==\n"
            + "-----END PRIVATE KEY-----";
    private final String[] CERTIFICATE_CHAIN = {
            "-----BEGIN CERTIFICATE-----\n" + "MIIDdzCCAl+gAwIBAgIED3hXJjANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV\n"
                    + "bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\n"
                    + "VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du\n"
                    + "MB4XDTIxMDQxMzA4MTQxOVoXDTIxMDcxMjA4MTQxOVowbDEQMA4GA1UEBhMHVW5r\n"
                    + "bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE\n"
                    + "ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC\n"
                    + "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIJxnU270BVgol2nqBx05vEK\n"
                    + "Kh945Vsm/dazX0lVro8ZNKBEQXLlz3v+T5KxIGEcAWnWffvfwTaB3ENq5+0qmZXd\n"
                    + "ln3esRBD4NvvU+LIWWsQS1wMT//XgQfaNocEiCHGpaaC9a7R7ylxri7GeY44kA8J\n"
                    + "ps1gsuzimdOH3Emf4H4x3P7802P8LsFXndbacByx1WqluYuN8KsNJ3+GB6XwCgBt\n"
                    + "2OJEaRgeSC8S7O2PHTWk7aIYCCmRy2KB3EX/KDDA47Fw8BLD/nlmNISAxnHs8zLE\n"
                    + "SIgvuOC39yAbrxCGVDed5h8n1x36Au+fWDV/Cwa0anDcyHd2CzoNCSvwcxBbRukC\n"
                    + "AwEAAaMhMB8wHQYDVR0OBBYEFCXFCTq9DDNw6jr0nE2VHw+6wqG0MA0GCSqGSIb3\n"
                    + "DQEBCwUAA4IBAQAjsKeIU0vf7vaOhUAMV60eP54kr6koiWBjhCxyKXQ+MECTFntn\n"
                    + "L459+uTFOCyoytWYjbe9ph79ossTWTCUUPCx9ZSaVdrpK5TyzXI+KBBWqGcLHxqc\n"
                    + "1jvU7zKLVf9oKGfhugnFvmj2EqC2vsrQPiG+p1RDfiLI9BqmhoDzBWzjZDdB6xt6\n"
                    + "PMAqecHfS24TzyWi8T4gLctcpSN22Aa394ky7sgBJPAQHWe7VWhRB0bVTZntwRsQ\n"
                    + "pEraINImKSw+m7MF/75s151yjKOzQxPZufl91oYyQMXoqX2fi0EUWo1oLm1x01dN\n"
                    + "L7w7ELyBzbNlk8a3dQc3Dcg+tu7VAf2tRtmc\n" + "-----END CERTIFICATE-----" };
    private final String JSON_MESSAGE_PUT_KEY = "{\n" + "   \"keystoreName\":\"MyKeystore\",\n"
            + "   \"alias\":\"myPrivateKey\",\n" + "   \"type\":\"PrivateKey\",\n" + "   \"privateKey\" : \""
            + PRIVATE_KEY + ",\n" + "   \"certificateChain\":[\"" + CERTIFICATE_CHAIN + "\"]\n" + "}";

    @Test(expected = KuraException.class)
    public void doGetTestNoResources() throws KuraException {
        KeystoreServiceRequestHandlerV1 handler = new KeystoreServiceRequestHandlerV1();

        List<String> resourcesList = Collections.emptyList();
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doGet(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoGetOtherwise() throws KuraException, NoSuchFieldException {
        KeystoreServiceRequestHandlerV1 handler = new KeystoreServiceRequestHandlerV1();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("test");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doGet(null, message);
    }

    @Test
    public void testDoGetKeystores() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                keystoreServices.put("MyKeystore", ksMock);
                try {
                    certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("keystores");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        List<String> responses = Arrays.asList(EMPTY_KEYSTORE_1, EMPTY_KEYSTORE_2);
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertTrue(responses.contains(new String(resPayload.getBody(), StandardCharsets.UTF_8)));
    }

    @Test
    public void testDoGetKeys() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        ByteArrayInputStream is = new ByteArrayInputStream(CERTIFICATE.getBytes());
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        ks.setCertificateEntry("alias", cert);
        Map<String, Entry> certs = new HashMap<>();
        certs.put("alias", new KeyStore.TrustedCertificateEntry(cert));
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntries()).thenReturn(certs);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                keystoreServices.put("MyKeystore", ksMock);
                try {
                    certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("keys");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        List<String> responses = Arrays.asList(KEYSTORE_ENTRY_1, KEYSTORE_ENTRY_2);
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertTrue(responses.contains(new String(resPayload.getBody(), StandardCharsets.UTF_8)));
    }

    @Test
    public void testDoGetKeysWithId()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        ByteArrayInputStream is = new ByteArrayInputStream(CERTIFICATE.getBytes());
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        ks.setCertificateEntry("alias", cert);
        Map<String, Entry> certs = new HashMap<>();
        certs.put("alias", new KeyStore.TrustedCertificateEntry(cert));
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntries()).thenReturn(certs);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                keystoreServices.put("MyKeystore", ksMock);
                try {
                    certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("keys");
        resourcesList.add("MyKeystore");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        List<String> responses = Arrays.asList(KEYSTORE_ENTRY_WITH_CERT_1, KEYSTORE_ENTRY_WITH_CERT_2);
        String response = new String(resPayload.getBody(), StandardCharsets.UTF_8);
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertTrue(responses.contains(response.substring(1, response.length() - 1)));
    }

    @Test
    public void testDoGetKey() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        ByteArrayInputStream is = new ByteArrayInputStream(CERTIFICATE.getBytes());
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        ks.setCertificateEntry("alias", cert);
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntry("alias")).thenReturn(new KeyStore.TrustedCertificateEntry(cert));

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                keystoreServices.put("MyKeystore", ksMock);
                try {
                    certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("keys");
        resourcesList.add("MyKeystore");
        resourcesList.add("alias");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        List<String> responses = Arrays.asList(KEYSTORE_ENTRY_WITH_CERT_1, KEYSTORE_ENTRY_WITH_CERT_2);
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertTrue(responses.contains(new String(resPayload.getBody(), StandardCharsets.UTF_8)));
    }

    @Test
    public void testDoPutTrustedCertificate()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                keystoreServices.put("MyKeystore", ksMock);
                try {
                    certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                assertEquals(JSON_MESSAGE_PUT_CERT, jsonString);
                CertificateInfo info = new CertificateInfo("myCertTest99", "MyKeystore");
                info.setType(EntryType.TRUSTED_CERTIFICATE);
                info.setCertificate(CERTIFICATE);
                return (T) info;
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("keys");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(JSON_MESSAGE_PUT_CERT.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doPut(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals("\"MyKeystore" + ":" + "myCertTest99\"", new String(resPayload.getBody(), StandardCharsets.UTF_8));
    }

    @Test(expected = WebApplicationException.class)
    public void testDoPutPrivateKey()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                keystoreServices.put("MyKeystore", ksMock);
                try {
                    certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                PrivateKeyInfo info = new PrivateKeyInfo("myPrivateKey", "MyKeystore");
                info.setType(EntryType.PRIVATE_KEY);
                info.setPrivateKey(PRIVATE_KEY);
                info.setCertificateChain(CERTIFICATE_CHAIN);
                return (T) info;
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("keys");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(JSON_MESSAGE_PUT_KEY.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doPut(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();
    }

    @Test
    public void testDoDel() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                keystoreServices.put("MyKeystore", ksMock);
                try {
                    certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                assertEquals(JSON_MESSAGE_DEL, jsonString);
                return (T) new EntryInfo("mycerttestec", "MyKeystore");
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("keys");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(JSON_MESSAGE_DEL.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doDel(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
    }
}
