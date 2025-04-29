'use client';

import Link from 'next/link';
import { useAuthStore } from '@/store/authStore';
import { useRouter } from 'next/navigation';

export default function Navigation() {
  const hasPermission = useAuthStore(state => state.hasPermission);
  const user = useAuthStore(state => state.user);
  const logout = useAuthStore(state => state.logout);
  const router = useRouter();

  // Function to check if menu item should be visible
  const isMenuItemVisible = (menuItem) => {
    if (user?.access === 'SALES') {
      // SALES users can see Home, Stock and Orders
      return menuItem === 'Stock' || menuItem === 'Orders' || menuItem === 'Home';
    }
    return true;
  };

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  const menuItems = [
    { name: 'Home', path: '/dashboard' },
    { name: 'Product Catalog', path: '/catalog' },
    { name: 'Stock', path: '/stock' },
    { name: 'Orders', path: '/orders' },
  ];

  const recordItems = [
    { name: 'All Records', path: '/records' },
    { name: 'In-Stock Barcode', path: '/records/in-stock' },
    { name: 'Logs', path: '/logs' },
  ];

  return (
    <div className="fixed inset-y-0 left-0 w-64 bg-white shadow-lg flex flex-col">
      {/* Logo */}
      <div className="flex items-center h-16 px-4 bg-white border-b border-gray-200">
        <span className="text-xl font-semibold text-black">Stock Manager</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-6 p-4">
        {/* Main Functions */}
        <div>
          <h2 className="px-4 text-xs font-semibold text-gray-900 uppercase tracking-wider">
            Main Functions
          </h2>
          <div className="mt-2 space-y-1">
            {menuItems.map((item) => (
              isMenuItemVisible(item.name) && (
                <Link 
                  key={item.path}
                  href={item.path} 
                  className="flex items-center px-4 py-2 text-gray-900 hover:bg-gray-100"
                >
                  <span>{item.name}</span>
                </Link>
              )
            ))}
          </div>
        </div>

        {/* Records - Only show for non-SALES users */}
        {user?.access !== 'SALES' && (
          <div>
            <h2 className="px-4 text-xs font-semibold text-gray-900 uppercase tracking-wider">
              Records
            </h2>
            <div className="mt-2 space-y-1">
              {recordItems.map((item) => (
                <Link 
                  key={item.path}
                  href={item.path} 
                  className="flex items-center px-4 py-2 text-gray-900 hover:bg-gray-100"
                >
                  <span>{item.name}</span>
                </Link>
              ))}
            </div>
          </div>
        )}
      </nav>

      {/* Logout Button */}
      <div className="p-4 border-t border-gray-200">
        <button
          onClick={handleLogout}
          className="w-full px-4 py-2 text-left text-red-600 hover:bg-red-50 rounded-md flex items-center"
        >
          <svg 
            xmlns="http://www.w3.org/2000/svg" 
            className="h-5 w-5 mr-2" 
            fill="none" 
            viewBox="0 0 24 24" 
            stroke="currentColor"
          >
            <path 
              strokeLinecap="round" 
              strokeLinejoin="round" 
              strokeWidth={2} 
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" 
            />
          </svg>
          Logout
        </button>
      </div>
    </div>
  );
} 