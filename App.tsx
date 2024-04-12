/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, {useState} from 'react';
import type {PropsWithChildren} from 'react';
import {
  NativeModules,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  useColorScheme,
  View,
} from 'react-native';

import {Colors} from 'react-native/Libraries/NewAppScreen';
import axios from 'axios';
import base64 from 'react-native-base64';
import {decode} from 'base-64';

type SectionProps = PropsWithChildren<{
  title: string;
}>;

function Section({children, title}: SectionProps): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  return (
    <View style={styles.sectionContainer}>
      <Text
        style={[
          styles.sectionTitle,
          {
            color: isDarkMode ? Colors.white : Colors.black,
          },
        ]}>
        {title}
      </Text>
      <Text
        style={[
          styles.sectionDescription,
          {
            color: isDarkMode ? Colors.light : Colors.dark,
          },
        ]}>
        {children}
      </Text>
    </View>
  );
}

function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  const [user, setUser] = useState();
  const [resp, setResp] = useState(false);

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };
  const transformToURLSafe = (challenge: string) => {
    const encodedChallenge = challenge
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/\=+$/, '');
    return encodedChallenge;
  };

  const transformBase64 = (responseId: string) => {
    let id = responseId;
    if (id.length % 4 !== 0) {
      id += '==='.slice(0, 4 - (id.length % 4));
    }
    id = id.replace(/-/g, '+').replace(/_/g, '/');
    return id;
  };

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        <TouchableOpacity
          style={{padding: 16}}
          onPress={async () => {
            try {
              const {data: datax} = await axios.post(
                'http://192.168.1.104:3000/api/auth/register',
                {username: '73047716'},
                {
                  headers: {
                    'Content-Type': 'application/json',
                  },
                },
              );
              const {data} = await axios.post(
                'http://192.168.1.104:3000/api/auth/create-register-challenge',
                {userId: datax.userId},
              );

              setUser(datax.userId);

              const {challengeToken, ...rest} = data;
              const {base64UserId, challenge} = JSON.parse(
                decode(challengeToken.split('.')[1]),
              );

              const response =
                await NativeModules.RNPasskeyModule.createCredential({
                  ...rest,
                  user: {
                    id: base64.decode(base64UserId),
                    name: base64.decode(data.user.name),
                    displayName: base64.decode(data.user.displayName),
                  },
                  challenge: transformToURLSafe(challenge),
                  timeout: 50000,
                });

              console.log('ANDROID REGITER', response);

              const {data: response2} = await axios.post(
                'http://192.168.1.104:3000/api/auth/confirm-register-challenge',
                {
                  id: transformBase64(response.id),
                  challengeToken: challengeToken,
                  rawId: transformBase64(response.rawId),
                  response: {
                    attestationObject: response.response.attestationObject,
                    clientDataJSON: response.response.clientDataJSON,
                  },
                },
              );

              console.log(response2);
            } catch (error) {
              console.error(error);
            }
          }}>
          <Text>register</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={{padding: 16}}
          onPress={async () => {
            const {data} = await axios.post(
              'http://192.168.1.104:3000/api/auth/create-auth-challenge',
              {
                userId: user,
              },
            );
            console.log('API', data);

            const {challengeToken} = data;
            const {challenge} = JSON.parse(
              decode(challengeToken.split('.')[1]),
            );
            const response = await NativeModules.RNPasskeyModule.signIn({
              challenge: transformToURLSafe(challenge),
              timeout: 50000,
              userVerification: data.userVerification,
              rpId: data.rpId,
              allowCredentials: [
                {
                  id: transformToURLSafe(data.allowCredentials[0].id),
                  type: data.allowCredentials[0].type,
                },
              ],
            });

            await axios.post(
              'http://192.168.1.104:3000/api/auth/confirm-auth-challenge',
              {
                challengeToken,
                id: transformBase64(response.id),
                rawId: transformBase64(response.rawId),
                response: {
                  ...response.response,
                  authenticatorData: transformBase64(
                    response.response.authenticatorData,
                  ),
                },
              },
            );

            setResp(true);
          }}>
          <Text>login</Text>
        </TouchableOpacity>
        {resp ? <Text>Logged In</Text> : null}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  sectionContainer: {
    marginTop: 32,
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '600',
  },
  sectionDescription: {
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
  },
  highlight: {
    fontWeight: '700',
  },
});

export default App;
