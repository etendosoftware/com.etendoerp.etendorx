INSERT INTO public.smfsws_config(smfsws_config_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, expirationtime, privatekey)
	VALUES ('0D9967787D6743DD9715860DE642171F','0','0','Y','2025-08-12 18:30:03.973','100','2025-08-12 18:30:03.973','100',0,'{"private-key":"-----BEGIN PRIVATE KEY-----MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgpMTI5Vn/foIbdcdsq0NL5Sv9kIvtwDuAZj8K90KtDKyhRANCAASVFCeRhD8PL7QMYC9Du2Sbm+bJwujOJvNJq11dSbeXkeiEN8llhPhdwfa3Hy3byxPCMWOQyzWmMzMPZncdBgs+-----END PRIVATE KEY-----","public-key":"-----BEGIN PUBLIC KEY-----MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAElRQnkYQ/Dy+0DGAvQ7tkm5vmycLozibzSatdXUm3l5HohDfJZYT4XcH2tx8t28sTwjFjkMs1pjMzD2Z3HQYLPg==-----END PUBLIC KEY-----"}');

INSERT INTO public.etrx_config(etrx_config_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, service_name, service_url, updateable_configs, public_url, restart_services)
	VALUES('2D18913D0D0747A794DEBC1EB1370156','0','0','Y','2025-08-12 20:49:11.745','100','2025-08-12 20:49:11.745','100','auth','http://auth:8094','Y','http://localhost:8094','Y');
INSERT INTO public.etrx_config(etrx_config_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, service_name, service_url, updateable_configs, public_url, restart_services)
	VALUES('79A4AF3BEE9D40CC8FD6A4F56AE9A22A','0','0','Y','2025-08-12 20:49:11.755','100','2025-08-12 20:49:11.755','100','edge','http://edge:8096','Y','http://localhost:8096','Y');
INSERT INTO public.etrx_config(etrx_config_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, service_name, service_url, updateable_configs, public_url, restart_services)
	VALUES('E1E5802D4ECE460997AA470317ADBE40','0','0','Y','2025-08-12 20:49:11.759','100','2025-08-12 20:49:11.759','100','config','http://config:8888','N','http://localhost:8888','Y');
INSERT INTO public.etrx_config(etrx_config_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, service_name, service_url, updateable_configs, public_url, restart_services)
	VALUES('F14B8721BBC44E72847D7CC28209318A','0','0','Y','2025-08-12 20:49:11.754','100','2025-08-12 20:49:11.754','100','das','http://das:8092','Y','http://localhost:8092','Y');

INSERT INTO public.etrx_service_param(etrx_service_param_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, param_value, param_key, etrx_config_id)
	VALUES ('084DF2BA23584161B4BAD5AD15B993A9','0','0','Y','2025-08-12 20:49:11.754','100','2025-08-12 20:49:11.754','100','http://das:8092','das.url','F14B8721BBC44E72847D7CC28209318A');
INSERT INTO public.etrx_service_param(etrx_service_param_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, param_value, param_key, etrx_config_id)
	VALUES ('26611A5270F147CC8B88CFB5D7F0EB2A','0','0','Y','2025-08-12 20:49:11.76','100','2025-08-12 20:49:11.76','100','http://das:8092','das.url','E1E5802D4ECE460997AA470317ADBE40');
INSERT INTO public.etrx_service_param(etrx_service_param_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, param_value, param_key, etrx_config_id)
	VALUES ('31A0C4EBD56842C88AD88E18E4985294','0','0','Y','2025-08-12 20:49:11.761','100','2025-08-12 20:49:11.761','100','http://tomcat:8080','classic.url','E1E5802D4ECE460997AA470317ADBE40');
INSERT INTO public.etrx_service_param(etrx_service_param_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, param_value, param_key, etrx_config_id)
	VALUES ('52BA8AC612C047FDB0024990366F300E','0','0','Y','2025-08-12 20:49:11.753','100','2025-08-12 20:49:11.753','100','http://tomcat:8080','classic.url','2D18913D0D0747A794DEBC1EB1370156');
INSERT INTO public.etrx_service_param(etrx_service_param_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, param_value, param_key, etrx_config_id)
	VALUES ('7B161910FA494279B3BCAB9A65CB50DD','0','0','Y','2025-08-12 20:49:11.751','100','2025-08-12 20:49:11.751','100','http://das:8092','das.url','2D18913D0D0747A794DEBC1EB1370156');
INSERT INTO public.etrx_service_param(etrx_service_param_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, param_value, param_key, etrx_config_id)
	VALUES ('80B0D6B1342B42449DCC41CD8B014715','0','0','Y','2025-08-12 20:49:11.758','100','2025-08-12 20:49:11.758','100','http://tomcat:8080','classic.url','79A4AF3BEE9D40CC8FD6A4F56AE9A22A');
INSERT INTO public.etrx_service_param(etrx_service_param_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, param_value, param_key, etrx_config_id)
	VALUES ('BB9BF660917842B986B59748A7C70E77','0','0','Y','2025-08-12 20:49:11.756','100','2025-08-12 20:49:11.756','100','http://das:8092','das.url','79A4AF3BEE9D40CC8FD6A4F56AE9A22A');
INSERT INTO public.etrx_service_param(etrx_service_param_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, param_value, param_key, etrx_config_id)
	VALUES ('CF4F2E63AAC14BEAA793924B90E9B0D3','0','0','Y','2025-08-12 20:49:11.755','100','2025-08-12 20:49:11.755','100','http://tomcat:8080','classic.url','F14B8721BBC44E72847D7CC28209318A');
