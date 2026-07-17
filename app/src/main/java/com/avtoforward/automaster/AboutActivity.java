package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbarAbout);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Информация");
        }

        ViewPager viewPager = findViewById(R.id.viewPagerAbout);
        TabLayout tabLayout = findViewById(R.id.tabLayoutAbout);

        AboutPagerAdapter adapter = new AboutPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Адаптер для вкладок
    private static class AboutPagerAdapter extends FragmentStatePagerAdapter {

        private final String[] titles = {"О приложении", "Политика", "Соглашение"};

        public AboutPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new AboutFragment();
                case 1: return new PrivacyFragment();
                case 2: return new TermsFragment();
                default: return new AboutFragment();
            }
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
    }

    // Фрагмент "О приложении"
    public static class AboutFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_about_webview, container, false);
            WebView webView = view.findViewById(R.id.webViewAbout);
            webView.setWebViewClient(new WebViewClient());
            String html = "<html><head><style>" +
                    "body{background:#121212;color:#ffffff;font-family:sans-serif;padding:16px;line-height:1.6;}" +
                    "h1{color:#FF9800;font-size:22px;}" +
                    "h2{color:#FF9800;font-size:18px;margin-top:16px;}" +
                    "p{color:#B3B3B3;font-size:14px;}" +
                    "ul{color:#B3B3B3;font-size:14px;}" +
                    "li{margin-bottom:8px;}" +
                    "</style></head><body>" +
                    "<h1>АвтоТехПомощь</h1>" +
                    "<p>Версия 1.0</p>" +
                    "<p>Приложение для быстрого вызова автомастеров: автоэлектриков, автомехаников, гидравликов, шиномонтаж и эвакуатор.</p>" +
                    "<h2>Как это работает</h2>" +
                    "<ul>" +
                    "<li>Выберите услугу и укажите адрес</li>" +
                    "<li>Мастер принимает заказ и выезжает к вам</li>" +
                    "<li>Вы можете общаться в форуме с другими мастерами и клиентами</li>" +
                    "</ul>" +
                    "<h2>Контакты</h2>" +
                    "<p>Email: support@avtotech.ru</p>" +
                    "<p>Телефон: +7 (999) 123-45-67</p>" +
                    "<p>© 2026 АвтоТехПомощь. Все права защищены.</p>" +
                    "</body></html>";
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            return view;
        }
    }

    // Фрагмент "Политика конфиденциальности"
    public static class PrivacyFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_about_webview, container, false);
            WebView webView = view.findViewById(R.id.webViewAbout);
            webView.setWebViewClient(new WebViewClient());
            String html = "<html><head><style>" +
                    "body{background:#121212;color:#ffffff;font-family:sans-serif;padding:16px;line-height:1.6;}" +
                    "h1{color:#FF9800;font-size:20px;}" +
                    "h2{color:#FF9800;font-size:16px;margin-top:12px;}" +
                    "p{color:#B3B3B3;font-size:14px;}" +
                    "ul{color:#B3B3B3;font-size:14px;padding-left:20px;}" +
                    "li{margin-bottom:6px;}" +
                    "</style></head><body>" +
                    "<h1>Политика конфиденциальности</h1>" +
                    "<p><b>Дата вступления в силу:</b> 24.06.2026</p>" +
                    "<h2>1. Общие положения</h2>" +
                    "<p>Настоящая Политика конфиденциальности (далее — Политика) действует в отношении всей информации, которую приложение «АвтоТехПомощь» (далее — Приложение) может получить о пользователе.</p>" +
                    "<h2>2. Какие данные собираются</h2>" +
                    "<ul>" +
                    "<li>Регистрационные данные: email, пароль</li>" +
                    "<li>Профиль пользователя: ФИО, телефон, город, никнейм, фото</li>" +
                    "<li>Для мастеров: фото паспорта для верификации</li>" +
                    "<li>Данные о заказах: адрес, описание проблемы</li>" +
                    "<li>Технические данные: IP-адрес, тип устройства</li>" +
                    "</ul>" +
                    "<h2>3. Цели сбора данных</h2>" +
                    "<ul>" +
                    "<li>Идентификация и авторизация пользователя</li>" +
                    "<li>Оказание услуг (создание и выполнение заказов)</li>" +
                    "<li>Общение в форуме</li>" +
                    "<li>Верификация мастеров</li>" +
                    "<li>Улучшение работы приложения</li>" +
                    "</ul>" +
                    "<h2>4. Передача данных третьим лицам</h2>" +
                    "<p>Мы не передаём ваши данные третьим лицам, за исключением случаев, предусмотренных законодательством РФ.</p>" +
                    "<h2>5. Права пользователя</h2>" +
                    "<ul>" +
                    "<li>Получить доступ к своим данным</li>" +
                    "<li>Изменить или удалить свои данные</li>" +
                    "<li>Отозвать согласие на обработку данных</li>" +
                    "</ul>" +
                    "<h2>6. Контакты</h2>" +
                    "<p>По всем вопросам, связанным с обработкой данных, обращайтесь по email: privacy@avtotech.ru</p>" +
                    "</body></html>";
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            return view;
        }
    }

    // Фрагмент "Пользовательское соглашение"
    public static class TermsFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_about_webview, container, false);
            WebView webView = view.findViewById(R.id.webViewAbout);
            webView.setWebViewClient(new WebViewClient());
            String html = "<html><head><style>" +
                    "body{background:#121212;color:#ffffff;font-family:sans-serif;padding:16px;line-height:1.6;}" +
                    "h1{color:#FF9800;font-size:20px;}" +
                    "h2{color:#FF9800;font-size:16px;margin-top:12px;}" +
                    "p{color:#B3B3B3;font-size:14px;}" +
                    "ul{color:#B3B3B3;font-size:14px;padding-left:20px;}" +
                    "li{margin-bottom:6px;}" +
                    "</style></head><body>" +
                    "<h1>Пользовательское соглашение</h1>" +
                    "<p><b>Дата вступления в силу:</b> 24.06.2026</p>" +
                    "<h2>1. Предмет соглашения</h2>" +
                    "<p>Настоящее Соглашение регулирует отношения между владельцем приложения «АвтоТехПомощь» и пользователем при использовании Приложения.</p>" +
                    "<h2>2. Права и обязанности пользователя</h2>" +
                    "<ul>" +
                    "<li>Использовать Приложение только для законных целей</li>" +
                    "<li>Предоставлять достоверные данные при регистрации</li>" +
                    "<li>Нести ответственность за сохранность пароля</li>" +
                    "<li>Не нарушать работу Приложения</li>" +
                    "</ul>" +
                    "<h2>3. Права и обязанности администрации</h2>" +
                    "<ul>" +
                    "<li>Обеспечивать работу Приложения</li>" +
                    "<li>Блокировать аккаунты за нарушения</li>" +
                    "<li>Изменять функционал Приложения без предварительного уведомления</li>" +
                    "</ul>" +
                    "<h2>4. Ограничение ответственности</h2>" +
                    "<p>Владелец Приложения не несёт ответственности за действия пользователей и мастеров, а также за качество оказанных услуг. Владелец не гарантирует бесперебойную работу Приложения.</p>" +
                    "<h2>5. Интеллектуальная собственность</h2>" +
                    "<p>Все материалы, размещённые в Приложении, являются интеллектуальной собственностью владельца.</p>" +
                    "<h2>6. Изменение условий</h2>" +
                    "<p>Владелец вправе изменять условия Соглашения с уведомлением пользователей.</p>" +
                    "<h2>7. Контакты</h2>" +
                    "<p>По всем вопросам обращайтесь по email: legal@avtotech.ru</p>" +
                    "</body></html>";
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            return view;
        }
    }
}