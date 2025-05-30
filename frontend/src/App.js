import React from 'react';
import { Routes, Route } from 'react-router-dom';
import LoginPage from './pages/login';
import HomePage from './pages/home';
import UserInfoSetting from "./pages/UserInfoSetting";
import Register from "./pages/register_avatar";
import MQTTConfigPage from "./pages/MQTTConfigSetting";
import SetTopicPage from "./pages/setTopic"

function App() {
  return (
      <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/home" element={<HomePage />} />
          <Route path="/register" element={<Register />} />
          <Route path="/userInfo" element={<UserInfoSetting />} />
          <Route path="/mqtt-config" element={<MQTTConfigPage />} />
          <Route path="/subscribe_topic" element={<SetTopicPage />} />
          <Route path="*" element={<LoginPage />} />
      </Routes>
  );
}

export default App;
