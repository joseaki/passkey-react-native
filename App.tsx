/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React from 'react';
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

import {
  Colors,
  DebugInstructions,
  Header,
  LearnMoreLinks,
  ReloadInstructions,
} from 'react-native/Libraries/NewAppScreen';
import axios from 'axios';
import base64 from 'react-native-base64';
import {decode, encode} from 'base64-arraybuffer';

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

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  async function postData(url = '', data = {}) {
    // Default options are marked with *
    const response = await fetch(url, {
      method: 'POST', // *GET, POST, PUT, DELETE, etc.
      mode: 'cors', // no-cors, *cors, same-origin
      cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
      credentials: 'same-origin', // include, *same-origin, omit
      headers: {
        'Content-Type': 'application/json',
        // 'Content-Type': 'application/x-www-form-urlencoded',
      },
      redirect: 'follow', // manual, *follow, error
      referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      body: JSON.stringify(data), // body data type must match "Content-Type" header
    });
    return response.json(); // parses JSON response into native JavaScript objects
  }

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
                'http://192.168.1.104:3000/api/auth/create-challenge',
                {userId: datax.userId},
              );

              const response =
                await NativeModules.RNPasskeyModule.createCredential({
                  ...data,
                  user: {
                    id: base64.decode(data.user.id),
                    name: base64.decode(data.user.name),
                    displayName: base64.decode(data.user.displayName),
                  },
                  challenge: transformToURLSafe(data.challenge),
                  timeout: 50000,
                });

              const {data: response2} = await axios.post(
                'http://192.168.1.104:3000/api/auth/respond-challenge',
                {
                  userId: datax.userId,
                  id: transformBase64(response.id),
                  rawId: response.rawId,
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
          onPress={() => {
            NativeModules.RNPasskeyModule.signIn({
              challenge: 'T1xCsnxM2DNL2KdK5CLa6fMhD7OBqho6syzInk_n-Uo',
              allowCredentials: [],
              timeout: 1800000,
              userVerification: 'required',
              rpId: 'joseaki.github.io',
            }).then(resp => {
              console.log('SIGNIN', resp);
            });
          }}>
          <Text>login</Text>
        </TouchableOpacity>
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
