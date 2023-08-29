from keycloak import KeycloakOpenID
import requests
import uuid

keycloak_openid = KeycloakOpenID(server_url='https://it030021.uni-graz.at/auth/', client_id='Cirilo', realm_name='Gams')

def getJWTFromKeycloakServer(username, passwd):
    token = keycloak_openid.token(username, passwd)
    return token['access_token']       

print("# Requesting FCGate API via Python")

# Get a token form keyclaok server
token = getJWTFromKeycloakServer('padawan', 'passwd')         

uuid    = str(uuid.uuid4())
params  = { 'pid': 'o:emile.tei' }
headers = { 'access_token': token, 'logid': uuid }

# Get a datastream list for object o:emile.tei
req = requests.post('https://it030021.uni-graz.at/fedora4api/getDatastreamListAsRDF', headers=headers, params=params)
print ('logid: ', uuid)
print (req.text)

# Get the datastream TEI_SOURCE of object o:emile.tei
req = requests.get('https://it030021.uni-graz.at/o:emile.tei/TEI_SOURCE')
print (req.text)

keycloak_openid.logout(token['refresh_token'])
print ('# Program teminated normally')


