import { create } from 'zustand';
import Cookies from 'js-cookie';
import axios from 'axios';
import { checkPermission } from '@/utils/permissions';

const API_URL = `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'}/auth`;

// Helper function to get user from cookie
const getUserFromCookie = () => {
  const userStr = Cookies.get('user');
  if (!userStr) return null;
  try {
    return JSON.parse(userStr);
  } catch (error) {
    console.error('Error parsing user data:', error);
    return null;
  }
};

export const useAuthStore = create((set, get) => ({
  user: getUserFromCookie(),
  token: Cookies.get('token') || null,
  isAuthenticated: !!Cookies.get('token'),

  hasPermission: (permission) => {
    const state = get();
    return state.user ? checkPermission(state.user.access, permission) : false;
  },

  login: async (username, password) => {
    try {
      console.log('Sending login request:', { username, password });
      const response = await axios.post(`${API_URL}/login`, {
        username,
        password,
      });

      console.log('Login response:', response.data);
      const { token, user } = response.data;
      
      // Store token and user data in cookies
      Cookies.set('token', token, { expires: 7 });
      Cookies.set('user', JSON.stringify(user), { expires: 7 });
      
      set({ user, token, isAuthenticated: true });
    } catch (error) {
      console.error('Login error details:', {
        status: error.response?.status,
        data: error.response?.data,
        message: error.message
      });
      throw error;
    }
  },

  logout: () => {
    Cookies.remove('token');
    Cookies.remove('user');
    set({ user: null, token: null, isAuthenticated: false });
  },
})); 