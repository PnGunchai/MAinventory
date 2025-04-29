import { Inter } from 'next/font/google'
import './globals.css'
import Link from 'next/link'

const inter = Inter({ subsets: ['latin'] })

export const metadata = {
  title: 'Inventory Management System',
  description: 'Stock management and inventory tracking system',
}

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <div className="min-h-screen bg-gray-100">
          {/* Sidebar */}
          <div className="fixed inset-y-0 left-0 w-64 bg-white shadow-lg">
            {/* Logo */}
            <div className="flex items-center h-16 px-4 bg-white border-b border-gray-200">
              <span className="text-xl font-semibold text-black">Stock Manager</span>
            </div>

            {/* Navigation */}
            <nav className="space-y-6 p-4">
              <div>
                <Link href="/" className="flex items-center px-4 py-2 text-gray-900 hover:bg-gray-100">
                  <span>Home</span>
                </Link>
              </div>

              {/* Main Functions */}
              <div>
                <h2 className="px-4 text-xs font-semibold text-gray-900 uppercase tracking-wider">
                  Main Functions
                </h2>
                <div className="mt-2 space-y-1">
                  <Link href="/catalog" className="flex items-center px-4 py-2 text-gray-900 hover:bg-gray-100">
                    <span>Product Catalog</span>
                  </Link>
                  <Link href="/stock" className="flex items-center px-4 py-2 text-gray-900 hover:bg-gray-100">
                    <span>Stock</span>
                  </Link>
                  <Link href="/orders" className="flex items-center px-4 py-2 text-gray-900 hover:bg-gray-100">
                    <span>Orders</span>
                  </Link>
                </div>
              </div>

              {/* Records */}
              <div>
                <h2 className="px-4 text-xs font-semibold text-gray-900 uppercase tracking-wider">
                  Records
                </h2>
                <div className="mt-2 space-y-1">
                  <Link href="/records" className="flex items-center px-4 py-2 text-gray-900 hover:bg-gray-100">
                    <span>All Records</span>
                  </Link>
                  <Link href="/logs" className="flex items-center px-4 py-2 text-gray-900 hover:bg-gray-100">
                    <span>Logs</span>
                  </Link>
                </div>
              </div>
            </nav>
          </div>

          {/* Main Content */}
          <div className="pl-64">
            <main className="p-8">
              {children}
            </main>
          </div>
        </div>
      </body>
    </html>
  )
}
