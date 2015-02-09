package es.progmac.cuadrante;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.DatePicker;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.codeslap.dateslider.DateSlider;
import com.codeslap.dateslider.MonthYearDateSlider;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import es.progmac.cuadrante.info.HoursInfo;
import es.progmac.cuadrante.info.ServicioInfo;
import es.progmac.cuadrante.info.TipoServicioInfo;
import es.progmac.cuadrante.info.TurnInfo;
import es.progmac.cuadrante.info.TurnTypeInfo;
import es.progmac.cuadrante.lib.CalendarAdapter;
import es.progmac.cuadrante.lib.Cuadrante;
import es.progmac.cuadrante.lib.CuadranteDates;
import es.progmac.cuadrante.lib.DatabaseHandler;
import es.progmac.cuadrante.lib.Extra;
import es.progmac.cuadrante.lib.IntvlMinTimePickerDialog;
import es.progmac.cuadrante.lib.MyLog;
import es.progmac.cuadrante.lib.Notify;
import es.progmac.cuadrante.lib.Sp;

/**
 * Página principal de la aplicación
 *
 * @author david
 */
public class MainActivity extends SherlockActivity {
    public static final String TAG = "MainActivity";
    /**
     * Mes actual para el calendario
     */
    public Calendar month;
    /**
     * Manejador del DataGrid
     */
    public CalendarAdapter adapter;
    public Handler handler;
    public ArrayList<String> items; // container to store some random calendar
    // items

    int requestCode;

    public DatabaseHandler db;

    static final int DIALOG_ID_SEARCH = 0;
    static final int DIALOG_ID_TYPE_SERVICE_DATE_RANGE = 1;
    static final int DIALOG_ID_ABOUT_ME = 2;
    private static final int DIALOG_ID_JUMP_TO_MONTH = 3;
    public static final int DIALOG_VOTE_FOR_ME = 6;
    private static final int DIALOG_ID_DELETE_DATE_RANGE_START = 7;
    private static final int DIALOG_ID_DELETE_DATE_RANGE_END = 8;
    private static final int DIALOG_ID_ASK_SCHEDULE = 9;
    private static final int DIALOG_ID_DATE_TURN = 10;
    /**
     * guarda el id del turno que se ha seleccionado de la lista
     */
    private int sSelectedTurnId = 0;
    /**
     * Guardara la fecha de inicio para eliminar por rango de fechas
     */
    private DateTime mDtDelRangeStart = new DateTime();
    /**
     * Guardará la fecha fin para eliminar por rango de fechas
     */
    private DateTime mDtDelRangeEnd = new DateTime();

    //Donde se guardara el tipo de servicio que se ha seleccionado para el
    //rango de fechas
    TipoServicioInfo tipoServicioDateRange;

    //guardará el dia seleccionado en el calendario
    /**
     * Guardará la celda en la cual ha hecho clic el usuario en el calendario
     */
    int selectedDay = 0;
    /**
     * guardara el dia seleccionado como tal, 01, 02, etc...
     */
    int selectedDay2;
    /**
     * type_id que se usa en el date_range de un typeId seleccionado
     */
    int typeId = 0;

    /**
     * ListNavigation items
     */
    private String[] mLocations;

    public Context mContext;
    /**
     * si no controlo el mostrar el diálogo de si está seguro el usuario de borrar los servicios
     * entre dos fechas, me muestra dicho diálogo dos veces. La única manera de impedirlo es usando
     * esta boolean para una vez mostrada la primera no mostrar la segunda.
     */
    public boolean mShowDialogDelRange = true;
    /**
     * Guarda el tipo de servicio parar las llamadas del timePicker de los servicios que salta
     * la selección de horas
     */
    private TipoServicioInfo mTypeService = new TipoServicioInfo();
    /**
     * Cuenta cuantos horarios se van mostrando
     */
    private int mCountSchedule = 0;
    private String mScheduleStart = "", mScheduleEnd = "";
    private String mScheduleStart2 = "", mScheduleEnd2 = "";
    private String mScheduleStart3 = "", mScheduleEnd3 = "";
    private String mScheduleStart4 = "", mScheduleEnd4 = "";
    /**
     * Guarda la hora y el minuto siguiente a mostrar en el timePicker
     */
    private int mNextScheduleHour = 0, mNextScheduleMinute = 0;
    private IntvlMinTimePickerDialog mTimePicker = null;

    /**
     * Guarda si se hace click en una celda del calendario
     */
    private boolean mIsItemClick = false;
    /**
     * Para saber si tenemos que mostrar el listado de turnos
     */
    private boolean sShowTurns = false;
    /**
     * Para saber si tenemos que mostrar el listado de opciones de inserción de turnos
     */
    private boolean sShowModeInsert = false;
    /**
     * Guarda el id del turno que se ha seleccionado de la lista
     */
    private int sTurnSelected = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DatabaseHandler(this);
        mContext = this;

        //para rellenar datos a patadas, vacia la bd y rellena muchos servicios
        //Cuadrante.setDBtoFill(this);
        //db.destroyServiceTable();
        //db.updateSchedules();
        //db.borrame();
        //db.destroyHoursTable();

        month = Calendar.getInstance();
        onNewIntent(getIntent());

        //de SearchResult saltamos a un mes en concreto
        Intent intent = getIntent();
        int extra_day = intent.getIntExtra(Extra.DAY, month.get(Calendar.DAY_OF_MONTH));
        int extra_month = intent.getIntExtra(Extra.MONTH, month.get(Calendar.MONTH));
        int extra_year = intent.getIntExtra(Extra.YEAR, month.get(Calendar.YEAR));
        month.set(extra_year, extra_month, 1);
        //MyLog.d(TAG, "onCreate year:" + month.get(Calendar.YEAR) + " month:" + month.get(Calendar.MONTH) + " day:" + month.get(Calendar.DAY_OF_MONTH));
        //si el dia actual del mes del movil es menor que el máximo del mes seleccionado por el 
        //user en la búsqueda pues ponemos el dia actual del mes en el movil
        if (extra_day <= month.getActualMaximum(Calendar.DAY_OF_MONTH))
            month.set(Calendar.DAY_OF_MONTH, extra_day);

        items = new ArrayList<String>();
        adapter = new CalendarAdapter(this, month);

        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(adapter);

        handler = new Handler();
        // adjuntamos al calendario la funcion this.calendarUpdater para
        // que se actualicen los dias
        handler.post(calendarUpdater);

        // ponemos la fecha actual al título del calendario mensual
        TextView title = (TextView) findViewById(R.id.title);
        title.setText(android.text.format.DateFormat.format("MMMM yyyy", month));

        TextView previous = (TextView) findViewById(R.id.previous);
        previous.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (month.get(Calendar.MONTH) == month
                        .getActualMinimum(Calendar.MONTH)) {
                    month.set((month.get(Calendar.YEAR) - 1),
                            month.getActualMaximum(Calendar.MONTH), 1);
                } else {
                    month.set(Calendar.MONTH, month.get(Calendar.MONTH) - 1);
                }
                refreshCalendar();
            }
        });

        TextView next = (TextView) findViewById(R.id.next);
        next.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (month.get(Calendar.MONTH) == month
                        .getActualMaximum(Calendar.MONTH)) {
                    month.set((month.get(Calendar.YEAR) + 1),
                            month.getActualMinimum(Calendar.MONTH), 1);
                } else {
                    month.set(Calendar.MONTH, month.get(Calendar.MONTH) + 1);
                }
                refreshCalendar();

            }
        });

        gridview.setLongClickable(true);
        registerForContextMenu(gridview);
        //se usa un textview invisible para poder mostrar el contextmenu al hacer click en un item
        //de la lista que sale al hacer clic en un dia del calendario, si no no se puede hacer
        registerForContextMenu(findViewById(R.id.contextmenu));
        //Mantener click en celda
        gridview.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v,
                                           int position, long id) {
                MyLog.d(TAG, "longClick " + mIsItemClick);
                TextView date = (TextView) v.findViewById(R.id.date);
                if (date instanceof TextView && !date.getText().equals("")) {
                    String day = date.getText().toString();
                    if (day.length() == 1) {
                        day = "0" + day;
                    }
                    String dateLong = android.text.format.DateFormat.format("yyyy-MM",
                            month) + "-" + day;
                    //MyLog.d(TAG, "long click:" + dateLong);
                    ServicioInfo service = db.getServicio(dateLong);
                    if (service.getId() > 0) {
                        mIsItemClick = false;
                        return false;

                    } else if (mIsItemClick || Sp.getLastUsedTypeService(mContext) == 0) {//simple click
                        mIsItemClick = false;
                        return false;
                    } else {//long click en celda vacía
                        //MyLog.d(TAG, "long click en celda vacia");
                        if (Cuadrante.canServiceSaves(mContext, Sp.getLastUsedTypeService(mContext), dateLong, dateLong)) {
                            Intent intent = new Intent(mContext, ServicioActivity.class);
                            // return chosen date as string format
                            intent.putExtra("ACTIVITY", "mainActivity");
                            intent.putExtra("ACTION", "selectTipo");
                            intent.putExtra("TYPE_ID", Sp.getLastUsedTypeService(mContext));
                            intent.putExtra("date", dateLong);
                            // setResult(RESULT_OK, intent);
                            // si se pone finish termina con la pantalla del calendario
                            // pero entonces no se puede volver para atrás con el botón
                            // del movil
                            // finish();
                            // startActivity(intent);
                            startActivityForResult(intent, requestCode);
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.error_cant_save_service_exceeded_limit, Toast.LENGTH_LONG).show();
                        }

                        mIsItemClick = false;
                        // Return true to consume the click event. In this case the
                        // onListItemClick listener is not called anymore.
                        return true;
                    }
                }
                mIsItemClick = false;
                return false;
            }
        });

        //cuando haces click a un dia del cuadrante
        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                mIsItemClick = true;
                TextView date = (TextView) v.findViewById(R.id.date);
                if (date instanceof TextView && !date.getText().equals("")) {
                    String day = date.getText().toString();
                    if (day.length() == 1) {
                        day = "0" + day;
                    }
                    String dateLong = android.text.format.DateFormat.format("yyyy-MM",
                            month) + "-" + day;
                    //MyLog.d(TAG, "sigle:" + dateLong);
                    ServicioInfo service = db.getServicio(dateLong);
                    if (service.getId() > 0) {
                        Intent intent = new Intent(MainActivity.this,
                                ServicioActivity.class);
                        // return chosen date as string format
                        intent.putExtra(
                                "date",
                                android.text.format.DateFormat.format("yyyy-MM",
                                        month) + "-" + day);
                        // setResult(RESULT_OK, intent);
                        // si se pone finish termina con la pantalla del calendario
                        // pero entonces no se puede volver para atrás con el botón
                        // del movil
                        // finish();
                        // startActivity(intent);
                        startActivityForResult(intent, requestCode);

                    } else {
                        openContextMenu(v);
                    }

                }
            }
        });

        //Stopwatch stopwatch = new Stopwatch();
        refreshResume();
        //stopwatch.stop();
        //MyLog.d("MainActivity", "duracion refreshResume:" + stopwatch.toString());
        Cuadrante.setFirstRun(this);
        Cuadrante.voteForMe(this, "OnCreate");
    }

    protected void OnPause() {
        Cuadrante.voteForMe(this, "OnPause");
    }

    @Override
    /**
     * Menu que sale al hacer click o mantener en una celda del calendario
     */
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        List<TurnInfo> turns = db.getAllTurns();
        if (sShowTurns) {//Listado de turnos
            sShowTurns = false;
            sShowModeInsert = true;
            menu.setHeaderTitle(R.string.select_turn);
            int i = 0;
            for (TurnInfo turn : turns) {
                menu.add(5, turn.getId(), i, turn.getName());
                i++;
            }
        } else if (sShowModeInsert) {//Listado de modos de inserción de turnos
            sShowModeInsert = false;
            menu.setHeaderTitle(R.string.select_turn_mode);
            menu.add(6, 0, 0, getString(R.string.turn_mode_block));
            menu.add(6, 1, 0, getString(R.string.turn_mode_date_range));
        } else {
            AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) menuInfo;
            //se guarda la posición de la celda del calendario para poder acceder
            //a él desde la funcion onContextItemSelected
            this.selectedDay = cmi.position;
            menu.setHeaderTitle(R.string.select_type_service);
            if (Sp.getEnterToService(mContext))
                menu.add(1, this.selectedDay, 0, R.string.enter_the_service);
            if (Sp.getCreateNewTypeService(mContext))
                menu.add(2, this.selectedDay, 0, R.string.create_new_type_service);
            menu.add(3, this.selectedDay, 0, R.string.mark_this_as_a_holiday);

            if (turns.size() > 0) {
                menu.add(4, Menu.NONE, Menu.NONE, R.string.insert_turn);
            }

            List<TipoServicioInfo> tipos_servicios = db.getAllTipoServicios();

            int i = 0;
            int groupId = 7;
            for (TipoServicioInfo tipo_servicio : tipos_servicios) {
                if (tipo_servicio.getIsDateRange() == 1) groupId = 8;
                else if (tipo_servicio.getAskSchedule() == 1) groupId = 9;
                else groupId = 7;
                menu.add(groupId, tipo_servicio.getId(), i,
                        tipo_servicio.getTitle());
                i++;
            }
        }

    }

    @Override
    /**
     * Que pasa al hacer click en una opción del menu que aparece al mantener
     * pulsado una celda del calendario
     */
    public boolean onContextItemSelected(android.view.MenuItem item) {
        //MyLog.d("onContextItemSelected", "dentro");
        GridView g = (GridView) findViewById(R.id.gridview);
        String day = (String) g.getItemAtPosition(this.selectedDay);
        this.selectedDay2 = Integer.valueOf(day);

        switch (item.getGroupId()) {
            case 1://entrar en el servicio
                if (!day.equals("")) {
                    Intent intent = new Intent(MainActivity.this,
                            ServicioActivity.class);

                    if (day.length() == 1) {
                        day = "0" + day;
                    }
                    // return chosen date as string format
                    intent.putExtra(
                            "date",
                            android.text.format.DateFormat.format("yyyy-MM",
                                    month) + "-" + day);
                    startActivityForResult(intent, requestCode);
                }
                break;
            case 2://crear nuevo tipo de servicio
                if (!day.equals("")) {
                    // Toast.makeText(this, "Action 1, Item "+s + " id:" +
                    // item.getItemId(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, TipoServicioActivity.class);
                    intent.putExtra("ACTIVITY", "mainActivity");
                    intent.putExtra("ACTION", "createTipo");
                    if (day.length() == 1) {
                        day = "0" + day;
                    }
                    intent.putExtra("DATE",
                            android.text.format.DateFormat.format("yyyy-MM", month)
                                    + "-" + day);
                    startActivityForResult(intent, Cuadrante.REQUEST_CODE_CREATE_TYPE);
                }
                break;
            case 3://Marca el día como festivo
                if (!day.equals("")) {
                    if (day.length() == 1) {
                        day = "0" + day;
                    }
                    String date = android.text.format.DateFormat.format("yyyy-MM", month) + "-" + day;
                    ServicioInfo servicio = db.getServicio(date);
                    if (servicio.getId() > 0) {
                        if (servicio.getIsHoliday() == 1) {
                            servicio.setAsHoliday(false, mContext);
                            //si el servicio está vinculado a un tipo volvemos a poner
                            //los colores de fondo y texto del tipo
                            if (servicio.getTypeId() > 0) {
                                TipoServicioInfo type_service = db.getTipoServicio(servicio.getTypeId());
                                servicio.setBgColor(type_service.getBgColor());
                                servicio.setTextColor(type_service.getTextColor());
                            }
                        } else {
                            servicio.setAsHoliday(true, mContext);
                        }
                        db.updateServicio(servicio);
                    } else {
                        ServicioInfo tmpService = new ServicioInfo(date, 1);
                        tmpService.setAsHoliday(true, mContext);
                        db.addService(tmpService);
                    }
                    Cuadrante.deleteHoursDate(mContext, date);
                    Cuadrante.refreshWidget(mContext);
                    refreshCalendar();
                }
                break;
            case 4://turnos
                sShowTurns = true;
                //openContextMenu(findViewById(R.id.contextmenu));//se cerraba el segundo menu, se abria y cerraba rapido
                findViewById(R.id.contextmenu).post(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.contextmenu).showContextMenu();
                    }
                });
                break;
            case 5://modo de insercion de turnos
                sShowModeInsert = true;
                sSelectedTurnId = item.getItemId();
                //openContextMenu(findViewById(R.id.contextmenu));//se cerraba el segundo menu, se abria y cerraba rapido
                findViewById(R.id.contextmenu).post(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.contextmenu).showContextMenu();
                    }
                });
                break;
            case 6://añadir turno
                switch (item.getItemId()) {
                    case 0://bloque
                        selectedDay2 = (selectedDay2 == 0) ? 1 : selectedDay2;
                        DateTime dt = new DateTime(
                                month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1, selectedDay2, 0, 0);
                        List<TurnTypeInfo> types = db.getTurnTypes(sSelectedTurnId);
                        for (TurnTypeInfo type : types) {
                            String date = CuadranteDates.formatDate(dt);
                            TipoServicioInfo typeService = db.getTipoServicio(type.getTypeId());
                            //si se excede no grabamos el servicio ese dia y continuamos
                            if (Cuadrante.canServiceSaves(mContext, typeService.getId(), date)) {
                                saveTypeToService(typeService, date);
                                if (type.getSaliente() == 1) {
                                    dt = dt.plusDays(1);
                                    date = CuadranteDates.formatDate(dt);
                                    //creo  un tipo temporal para así poder usar la funcion saveTypeToService
                                    //y no tener que crear una nueva
                                    TipoServicioInfo typeSaliente =
                                            new TipoServicioInfo("", Cuadrante.CALENDAR_SERVICE_SALIENTE,
                                                    0, typeService.getBgColor(), typeService.getTextColor(),
                                                    Cuadrante.SCHEDULE_NULL, Cuadrante.SCHEDULE_NULL,
                                                    Cuadrante.SCHEDULE_NULL, Cuadrante.SCHEDULE_NULL,
                                                    Cuadrante.SCHEDULE_NULL, Cuadrante.SCHEDULE_NULL,
                                                    Cuadrante.SCHEDULE_NULL, Cuadrante.SCHEDULE_NULL, 0, 0,
                                                    0, 0);
                                    saveTypeToService(typeSaliente, date);
                                }
                            }
                            dt = dt.plusDays(1);
                        }
                        Cuadrante.refreshWidget(mContext);
                        db.destroyHoursTable();
                        refreshCalendar();
                        break;
                    case 1://fecha
                        showDialog(DIALOG_ID_DATE_TURN);
                        break;
                }
                break;
            case 7:
                if (!day.equals("")) {
                    // Toast.makeText(this, "Action 1, Item "+s + " id:" +
                    // item.getItemId(), Toast.LENGTH_SHORT).show();
                    if (day.length() == 1) {
                        day = "0" + day;
                    }
                    String date = android.text.format.DateFormat.format("yyyy-MM", month)
                            + "-" + day;
                    if (Cuadrante.canServiceSaves(this, item.getItemId(), date, date)) {
                        Intent intent = new Intent(this, ServicioActivity.class);
                        // return chosen date as string format
                        intent.putExtra("ACTIVITY", "mainActivity");
                        intent.putExtra("ACTION", "selectTipo");
                        intent.putExtra("TYPE_ID", item.getItemId());
                        intent.putExtra("date", date);
                        Sp.setLastUsedTypeService(mContext, item.getItemId());
                        // setResult(RESULT_OK, intent);
                        // si se pone finish termina con la pantalla del calendario
                        // pero entonces no se puede volver para atrás con el botón
                        // del movil
                        // finish();
                        // startActivity(intent);
                        startActivityForResult(intent, requestCode);
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.error_cant_save_service_exceeded_limit, Toast.LENGTH_LONG).show();
                    }
                }
                break;
            //grupo de tipos de servicio que al hacer click sale el DatePicker
            //para elegir el rango de fechas
            case 8://servicio como rango de fechas
                if (!day.equals("")) {
                    this.typeId = item.getItemId();
                    showDialog(DIALOG_ID_TYPE_SERVICE_DATE_RANGE);
                }
                break;
            case 9:
                if (!day.equals("")) {
                    this.typeId = item.getItemId();
                    showDialog(DIALOG_ID_ASK_SCHEDULE);
                }
                break;
        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);
        //si está logueado mostrar el botón de desloguarse en la barra de título
        if (Sp.getLoginActivate(this)) {
            //getMenuInflater().inflate(R.menu.activity_main_signin, menu);
            menu.add(Menu.NONE, R.id.menu_logout, Menu.NONE, R.string.logout)
                    .setIcon(R.drawable.ic_action_logout)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intentSettings = new Intent(this, SettingsActivity.class);
                startActivity(intentSettings);
                return true;
            case R.id.menu_type_service:
                Intent intent = new Intent(MainActivity.this,
                        TypeServiceListActivity.class);
                setResult(RESULT_OK, intent);
                // si se pone finish termina con la pantalla del calendario
                // pero entonces no se puede volver para atrás con el botón
                // del movil
                // finish();
                // startActivity(intent);
                startActivityForResult(intent, requestCode);
                return true;
            case R.id.menu_comisiones:
                Intent intentComisiones = new Intent(this, ComisionListActivity.class);
                startActivity(intentComisiones);
                return true;
            case R.id.menu_search_per_comments:
                showDialog(DIALOG_ID_SEARCH);
                //Intent intentSearch = new Intent(this, SearchActivity.class);
                //startActivity(intentSearch);
                return true;
            case R.id.menu_about_me:
                //showDialog(DIALOG_ID_ABOUT_ME);
                //showInflateDialog(DIALOG_ID_FIRSTSTEPS);
                Cuadrante.showInflateDialog(this, Cuadrante.DIALOG_ID_WHATSNEW);
                return true;
            case R.id.menu_jump_to_month:
                showDialog(DIALOG_ID_JUMP_TO_MONTH);
                return true;
            case R.id.menu_logout:
                Cuadrante.logOut(this);
                System.exit(0);
                return true;
            case R.id.menu_help:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Cuadrante.URL_HELP));
                startActivity(browserIntent);
                return true;
            case R.id.menu_schedule:
                boolean view_schedule = adapter.getViewSchedule();
                if (view_schedule) view_schedule = false;
                else view_schedule = true;
                adapter.setViewSchedule(view_schedule);
                refreshCalendar(false);
                adapter.notifyDataSetChanged();
                return true;
            case R.id.menu_contact:
                //creamos la pantalla de envio de email
                Intent email = new Intent(Intent.ACTION_SEND);
                email.putExtra(Intent.EXTRA_EMAIL, new String[]{Cuadrante.EMAIL_CONTACT});
                email.putExtra(Intent.EXTRA_SUBJECT,
                        getResources().getString(R.string.email_contact_subject));
                email.putExtra(Intent.EXTRA_TEXT,
                        getResources().getString(R.string.email_contact_message));
                email.setType("message/rfc822");
                startActivityForResult(Intent.createChooser(email, getResources().getString(R.string.choose_email_client)), Cuadrante.REQUEST_CODE_EMAIL);

                return true;
            case R.id.menu_delete_date_range:
                showDialog(DIALOG_ID_DELETE_DATE_RANGE_START);
                return true;
            case R.id.menu_today:

                Calendar today = Calendar.getInstance();
                month.set(
                        today.get(Calendar.YEAR),
                        today.get(Calendar.MONTH),
                        1);//si pone el today.get(Calendar.DAY_OF_MONTH) no muestra el calendar correctamente
                //MyLog.d(TAG, "menu_today year:" + month.get(Calendar.YEAR) + " month:" + month.get(Calendar.MONTH) + " day:" + month.get(Calendar.DAY_OF_MONTH));
                refreshCalendar();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new DatePickerDialog(this, datePickerListener, month.get(Calendar.YEAR), month.get(Calendar.MONTH) - 1, this.selectedDay2);
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_ID_SEARCH:
                Intent intent = new Intent(mContext, SearchActivity.class);
                startActivity(intent);
                /*
				Resources res = getResources();
				PromptDialog dlg2 = new PromptDialog(this,
						R.string.title_search, 0, null, 
						res.getInteger(R.integer.MAX_LENGTH_INPUT_SEARCH), 
						String.format(this.getString(R.string.hint_max_characters),
								res.getInteger(R.integer.MAX_LENGTH_INPUT_SEARCH)),0) {
					@Override
					public boolean onOkClicked(String input) {
						if(input.length() > 0){
							Intent intent2 = new Intent(MainActivity.this,
									SearchResultActivity.class);
							intent2.putExtra("searchString", input);
							//setResult(RESULT_OK, intent2);
							startActivityForResult(intent2, requestCode);
						}else{//no ha introducido texto.
							Toast.makeText(MainActivity.this, R.string.error_no_input_search, Toast.LENGTH_LONG).show();
						}
						return true; // true = close dialog
					}
				};
				dlg2.show();
				*/
                break;
            case DIALOG_ID_TYPE_SERVICE_DATE_RANGE:
                //MyLog.d("onCreateDialog", "fecha:" + month.get(Calendar.YEAR) + "-" + month.get(Calendar.MONTH) + "-" +this.selectedDay2);
                Toast.makeText(this, R.string.select_date_range, Toast.LENGTH_LONG).show();
                return new DatePickerDialog(this, datePickerListener,
                        month.get(Calendar.YEAR), month.get(Calendar.MONTH), this.selectedDay2);

            case DIALOG_ID_ABOUT_ME:
                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                // 2. Chain together various setter methods to set the dialog characteristics
                String versionName = getResources().getString(R.string.app_versionName);
                builder.setMessage(String.format(this.getString(R.string.dialog_about_me), versionName, Cuadrante.APP_VERSION_DATE))
                        .setTitle(R.string.app_name);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                    }
                });
                // 3. Get the AlertDialog from create()
                AlertDialog dialog = builder.create();
                dialog.show();
                break;

            case DIALOG_ID_JUMP_TO_MONTH:
                return new MonthYearDateSlider(this, mMonthYearSetListener, month);

            case DIALOG_VOTE_FOR_ME:
                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog.Builder builderVote = new AlertDialog.Builder(this);

                // 2. Chain together various setter methods to set the dialog characteristics
                builderVote.setMessage(R.string.dialog_message_vote_for_me)
                        .setTitle(R.string.dialog_title_vote_for_me);
                // Add the buttons
                builderVote.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Cuadrante.APP_GOOGLE_PLAY_URL));
                        startActivity(browserIntent);
                    }
                });
                builderVote.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
                // 3. Get the AlertDialog from create()
                AlertDialog dialogVote = builderVote.create();
                dialogVote.show();
                break;
            case DIALOG_ID_DELETE_DATE_RANGE_START:
                //MyLog.d("onCreateDialog", "fecha:" + month.get(Calendar.YEAR) + "-" + month.get(Calendar.MONTH) + "-" +this.selectedDay2);
                Toast.makeText(this, R.string.select_delete_date_range_start, Toast.LENGTH_LONG).show();
                return new DatePickerDialog(this, datePickerDeleteStartListener,
                        month.get(Calendar.YEAR), month.get(Calendar.MONTH), 1);
            case DIALOG_ID_DELETE_DATE_RANGE_END:
                //MyLog.d("onCreateDialog", "fecha:" + month.get(Calendar.YEAR) + "-" + month.get(Calendar.MONTH) + "-" +this.selectedDay2);
                Toast.makeText(this, R.string.select_delete_date_range_end, Toast.LENGTH_LONG).show();
                return new DatePickerDialog(this, datePickerDeleteEndListener,
                        month.get(Calendar.YEAR), month.get(Calendar.MONTH), 1);
            case DIALOG_ID_ASK_SCHEDULE:
                MyLog.d(TAG, "DIALOG_ID_ASK_SCHEDULE");
                mTypeService = db.getTipoServicio(typeId);
                resetTimePickerValues();
                setNextShedule(mTypeService.getStartSchedule());
                return showTimePicker();
            case DIALOG_ID_DATE_TURN:
                Toast.makeText(this, R.string.selecte_turn_date_end, Toast.LENGTH_LONG).show();
                return new DatePickerDialog(this, datePickerTurnListener,
                        month.get(Calendar.YEAR), month.get(Calendar.MONTH), this.selectedDay2);

        }
        return null;
    }

    /**
     * Inicializa valores necesarios para el timePicker de las horas de servicios a añadir
     */
    public void resetTimePickerValues() {
        mTimePicker = null;
        mCountSchedule = 0;
        mNextScheduleHour = mNextScheduleMinute = 0;
        mScheduleStart = mScheduleEnd =
                mScheduleStart2 = mScheduleEnd2 =
                        mScheduleStart3 = mScheduleEnd3 =
                                mScheduleStart4 = mScheduleEnd4 = "";
    }

    public void setNextShedule(String schedule) {
        if (!schedule.equals(Cuadrante.SCHEDULE_NULL)) {
            mNextScheduleHour = CuadranteDates.getHour(schedule);
            mNextScheduleMinute = CuadranteDates.getMinutes(schedule);
        } else {
            mNextScheduleHour = CuadranteDates.getHour(Cuadrante.SCHEDULE_DV);
            mNextScheduleMinute = CuadranteDates.getMinutes(Cuadrante.SCHEDULE_DV);
        }
    }

    public IntvlMinTimePickerDialog showTimePicker() {
        MyLog.d(TAG, "Dentro de showTimePicker:" + mTimePicker);
        if (mTimePicker == null) {
            mTimePicker = new IntvlMinTimePickerDialog(
                    mContext, mTimeSetListener,
                    mNextScheduleHour,
                    IntvlMinTimePickerDialog.getRoundedMinute(mNextScheduleMinute, mContext),
                    true);
            mTimePicker.setCancelable(true);
        }
        return mTimePicker;
    }

    @Override
    /**
     * Se llama cuando es llamado un dialogo por segunda vez y que la primera
     * vez que un dialogo es llamado se queda en memoria y siempre muestra el
     * mismo cuando lo llames por segunda, tercera, etc...
     * Con esto conseguimos que vuelva a llamarse
     */
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_ID_TYPE_SERVICE_DATE_RANGE:
            case DIALOG_ID_DATE_TURN:
                ((DatePickerDialog) dialog).updateDate(month.get(Calendar.YEAR), month.get(Calendar.MONTH), this.selectedDay2);
                break;
            case DIALOG_ID_DELETE_DATE_RANGE_START:
            case DIALOG_ID_DELETE_DATE_RANGE_END:
                ((DatePickerDialog) dialog).updateDate(month.get(Calendar.YEAR), month.get(Calendar.MONTH), 1);
                break;
            case DIALOG_ID_ASK_SCHEDULE:
                mTypeService = db.getTipoServicio(typeId);
                resetTimePickerValues();
                setNextShedule(mTypeService.getStartSchedule());
                //MyLog.d(TAG, "onPrepareDialog de Ask Shedule");
                //MyLog.d(TAG, "timePicker: " + mTimePicker);
                break;
        }
    }

    /**
     * Dialog de selección de horario
     */
    private IntvlMinTimePickerDialog.OnTimeSetListener mTimeSetListener =
            new IntvlMinTimePickerDialog.OnTimeSetListener() {
                private String mStart = ""
                        ,
                        mEnd = "";

                public void resetValues() {
                    mTimePicker = null;
                    mStart = mEnd = "";
                }

                public void saveService() {
			/*
			Toast.makeText(mContext, 
					mScheduleStart + " " + mScheduleEnd + " " + 
					mScheduleStart2 + " " + mScheduleEnd2 + " " + 
					mScheduleStart3 + " " + mScheduleEnd3 + " " + 
					mScheduleStart4 + " " + mScheduleEnd4, Toast.LENGTH_LONG).show();
			*/
                    String date = android.text.format.DateFormat.format("yyyy-MM", month)
                            + "-" + CuadranteDates.pad(selectedDay2);
                    MyLog.d(TAG, selectedDay + " " + selectedDay2);
                    if (Cuadrante.canServiceSaves(mContext, typeId, date, date)) {
                        Intent intent = new Intent(mContext, ServicioActivity.class);
                        // return chosen date as string format
                        intent.putExtra("ACTIVITY", "mainActivity");
                        intent.putExtra("ACTION", "selectTipo");
                        intent.putExtra("TYPE_ID", typeId);
                        intent.putExtra("date", date);
                        intent.putExtra("SCHEDULE_START", mScheduleStart);
                        intent.putExtra("SCHEDULE_END", mScheduleEnd);
                        // setResult(RESULT_OK, intent);
                        // si se pone finish termina con la pantalla del calendario
                        // pero entonces no se puede volver para atrás con el botón
                        // del movil
                        // finish();
                        // startActivity(intent);
                        startActivityForResult(intent, requestCode);
                        Cuadrante.refreshWidget(mContext);
                        refreshCalendar();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.error_cant_save_service_exceeded_limit, Toast.LENGTH_LONG).show();
                    }
                    resetTimePickerValues();
                    resetValues();
                }

                public void showQuestion() {
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
                    dialog.setTitle(R.string.title_dialog_add_schedule);
                    dialog.setMessage(getString(R.string.message_alert_add_schedule, mStart, mEnd));
                    dialog.setCancelable(true);
                    dialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            resetValues();
                            showTimePicker().show();
                        }
                    });
                    dialog.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            resetValues();
                            dialog.cancel();
                            saveService();
                        }
                    });
                    dialog.create();
                    dialog.show();
                }

                public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfHour) {
                    MyLog.d(TAG, hourOfDay + ":" + minuteOfHour + " CountSchedule: " + mCountSchedule);
                    //TODO Mirar porque en algunas versiones no funciona
                    //no se porque, pero al darle al ok del dialog del timepicker me llama a esta funcion
                    //onTimeSet dos veces seguidas. del 4.1 para arriba
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        switch (mCountSchedule) {
                            case 0://scheduleStart
                                mStart = mScheduleStart = CuadranteDates.formatTime(hourOfDay, minuteOfHour);
                                setNextShedule(mTypeService.getEndSchedule());
                                showTimePicker().show();
                                break;
                            case 2://scheduleEnd
                                mEnd = mScheduleEnd = CuadranteDates.formatTime(hourOfDay, minuteOfHour);
                                setNextShedule(mTypeService.getStartSchedule2());
                                //showQuestion();
                                saveService();
                                break;
                        }
                        mCountSchedule++;
                    } else {
                        switch (mCountSchedule) {
                            case 0://scheduleStart
                                mStart = mScheduleStart = CuadranteDates.formatTime(hourOfDay, minuteOfHour);
                                setNextShedule(mTypeService.getEndSchedule());
                                showTimePicker().show();
                                break;
                            case 1://scheduleEnd
                                mEnd = mScheduleEnd = CuadranteDates.formatTime(hourOfDay, minuteOfHour);
                                setNextShedule(mTypeService.getStartSchedule2());
                                saveService();
                                break;
                        }
                        mCountSchedule++;
				/*
				//si es par significa que hemos guardado el scheduleEnd adecuado
				if(mCountSchedule == 2 || mCountSchedule == 4 || mCountSchedule == 6){		
					showQuestion();			    		
				}else if(mCountSchedule == 8){
					saveService();
				}else{
					showTimePicker().show();				
				}
				*/

                    }

                }
            };


    /**
     * datePicker de Inicio para eliminar servicios por rango de fechas
     */
    private DatePickerDialog.OnDateSetListener datePickerDeleteStartListener =
            new DatePickerDialog.OnDateSetListener() {
                // when dialog box is closed, below method will be called.
                public void onDateSet(DatePicker view, int selectedYear,
                                      int selectedMonth, int selectedDay) {
                    mDtDelRangeStart = new DateTime(selectedYear, selectedMonth + 1, selectedDay, 0, 0);
                    showDialog(DIALOG_ID_DELETE_DATE_RANGE_END);
                }
            };
    /**
     * datePicker de Fin para eliminar servicios por rango de fechas
     */
    private DatePickerDialog.OnDateSetListener datePickerDeleteEndListener =
            new DatePickerDialog.OnDateSetListener() {

                public void showQuestion() {
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
                    dialog.setTitle(R.string.title_dialog_delete_date_range);
                    dialog.setMessage(
                            getString(
                                    R.string.message_alert_delete_date_range,
                                    mDtDelRangeStart.toString("dd-MM-Y"),
                                    mDtDelRangeEnd.toString("dd-MM-Y")));
                    dialog.setCancelable(false);
                    dialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mShowDialogDelRange = true;
                            db.deleteServicesByDateRange(
                                    CuadranteDates.formatDate(mDtDelRangeStart),
                                    CuadranteDates.formatDate(mDtDelRangeEnd));
                            Cuadrante.refreshWidget(mContext);
                            db.destroyHoursTable();
                            refreshCalendar();
                            refreshResume();
                        }
                    });
                    dialog.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mShowDialogDelRange = true;
                        }
                    });
                    dialog.create();
                    dialog.show();
                }

                // when dialog box is closed, below method will be called.
                public void onDateSet(DatePicker view, int selectedYear,
                                      int selectedMonth, int selectedDay) {
                    mDtDelRangeEnd = new DateTime(selectedYear, selectedMonth + 1, selectedDay, 0, 0);
                    //si se ha equivocado y las fechas son al revés
                    if (mDtDelRangeStart.isAfter(mDtDelRangeEnd)) {
                        DateTime tmp = mDtDelRangeStart;
                        mDtDelRangeStart = mDtDelRangeEnd;
                        mDtDelRangeEnd = tmp;
                    }
                    if (mShowDialogDelRange) {
                        mShowDialogDelRange = false;
                        showQuestion();
                    }
                }
            };
    /**
     * datePicker de creación de servicios por rango de fechas
     */
    private DatePickerDialog.OnDateSetListener datePickerListener =
            new DatePickerDialog.OnDateSetListener() {
                // when dialog box is closed, below method will be called.
                public void onDateSet(DatePicker view, int selectedYear,
                                      int selectedMonth, int selectedDay) {
                    selectedDay2 = (selectedDay2 == 0) ? 1 : selectedDay2;
                    DateTime dStart = new DateTime(month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1, selectedDay2, 0, 0);
                    DateTime dEnd = new DateTime(selectedYear, selectedMonth + 1, selectedDay, 0, 0);
			/*
			MyLog.d("onDateSet", dStart.toString());
			MyLog.d("onDateSet", dEnd.toString());
			*/
                    //si la fecha de inicio es mayor eso es un error por lo que
                    //volvemos a mostrar el datepicker
                    if (dStart.isBefore(dEnd) || dStart.isEqual(dEnd)) {
                        TipoServicioInfo typeService = db.getTipoServicio(typeId);
                        if (typeService.getId() > 0) {
                            //miramos si se puede grabar el servicio entre las fechas
                            //seleccionadas
                            if (Cuadrante.canServiceSaves(MainActivity.this, typeService.getId(), dStart, dEnd)) {
                                while (dStart.isBefore(dEnd) || dStart.isEqual(dEnd)) {
                                    String date = CuadranteDates.formatDate(dStart.getYear(),
                                            dStart.getMonthOfYear(), dStart.getDayOfMonth());
                                    saveTypeToService(typeService, date);
                                    //MyLog.d("onDateSet", dStart.toString());
                                    Cuadrante.deleteHoursDate(mContext, date);
                                    dStart = dStart.plusDays(1);
                                }
                                Cuadrante.refreshWidget(mContext);
                                //Cuadrante.deleteHoursDate(mContext, CuadranteDates.formatDate(dStart));
                                //Cuadrante.deleteHoursDate(mContext, CuadranteDates.formatDate(dEnd));
                            } else {
                                Toast.makeText(getApplicationContext(), R.string.error_cant_save_service_exceeded_limit, Toast.LENGTH_LONG).show();
                            }
                        }
                        refreshCalendar();
                    }

                }
            };

    /**
     * datePicker de creación de turnos
     */
    private DatePickerDialog.OnDateSetListener datePickerTurnListener =
            new DatePickerDialog.OnDateSetListener() {
                // when dialog box is closed, below method will be called.
                public void onDateSet(DatePicker view, int selectedYear,
                                      int selectedMonth, int selectedDay) {
                    selectedDay2 = (selectedDay2 == 0) ? 1 : selectedDay2;
                    DateTime dStart = new DateTime(
                            month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1, selectedDay2, 0, 0);
                    DateTime dEnd = new DateTime(selectedYear, selectedMonth + 1, selectedDay, 0, 0);
                    //si la fecha de inicio es mayor eso es un error por lo que
                    //volvemos a mostrar el datepicker
                    if (dStart.isBefore(dEnd) || dStart.isEqual(dEnd)) {
                        List<TurnTypeInfo> types = db.getTurnTypes(sSelectedTurnId);
                        int i = 0;
                        boolean isSaliente = false;
                        //hay que mirar el tamaño del array de turnos porque da error en algunos moviles
                        //al entrar y turnos hay 0
                        while (dStart.isBefore(dEnd) || dStart.isEqual(dEnd) && types.size() > 0) {
                            TurnTypeInfo type = types.get(i);
                            String date = CuadranteDates.formatDate(
                                    dStart.getYear(), dStart.getMonthOfYear(), dStart.getDayOfMonth());
                            TipoServicioInfo typeService = db.getTipoServicio(type.getTypeId());
                            if (!isSaliente) {
                                //si se excede no grabamos el servicio ese dia y continuamos
                                if (Cuadrante.canServiceSaves(MainActivity.this, typeService.getId(), date, date)) {
                                    saveTypeToService(typeService, date);
                                    if (type.getSaliente() == 1) isSaliente = true;
                                } else {
                                    isSaliente = false;
                                }
                            } else {
                                //creo  un tipo temporal para así poder usar la funcion saveTypeToService
                                //y no tener que crear una nueva
                                TipoServicioInfo typeSaliente =
                                        new TipoServicioInfo("", Cuadrante.CALENDAR_SERVICE_SALIENTE,
                                                0, typeService.getBgColor(), typeService.getTextColor(),
                                                Cuadrante.SCHEDULE_NULL, Cuadrante.SCHEDULE_NULL,
                                                Cuadrante.SCHEDULE_NULL, Cuadrante.SCHEDULE_NULL,
                                                Cuadrante.SCHEDULE_NULL, Cuadrante.SCHEDULE_NULL,
                                                Cuadrante.SCHEDULE_NULL, Cuadrante.SCHEDULE_NULL, 0, 0, 0,
                                                0);
                                saveTypeToService(typeSaliente, date);
                                isSaliente = false;
                            }
                            if (!isSaliente) {
                                //si hay que insertar un saliente no cambiamos de tipo
                                i++;
                                if (i >= types.size()) i = 0;
                            }
                            dStart = dStart.plusDays(1);
                        }
                        Cuadrante.refreshWidget(mContext);
                        db.destroyHoursTable();
                        refreshCalendar();
                    }
                }
            };

    /**
     * Graba los datos de un tipo en un servicio
     *
     * @param typeService
     * @param date
     */
    private void saveTypeToService(TipoServicioInfo typeService, String date) {
        ServicioInfo service = db.getServicio(date);
        if (service.getId() > 0) {
            //db.deleteServicio(service.getId());
            String bgColor = "", textColor = "";
            if (service.getIsHoliday() == 1) {
                bgColor = service.getBgColor();
                textColor = service.getTextColor();
            } else {
                bgColor = typeService.getBgColor();
                textColor = typeService.getTextColor();
            }
            db.updateServicio(new ServicioInfo(date,
                    typeService.getId(),//typeId,
                    typeService.getName(),
                    bgColor,
                    textColor,
                    typeService.getStartSchedule(),
                    typeService.getEndSchedule(),
                    typeService.getStartSchedule2(),
                    typeService.getEndSchedule2(),
                    typeService.getStartSchedule3(),
                    typeService.getEndSchedule3(),
                    typeService.getStartSchedule4(),
                    typeService.getEndSchedule4(),
                    service.getComments(),
                    service.getIsHoliday(),
                    typeService.getGuardiaCombinada(),
                    typeService.getTypeDay(),
                    service.getIsImportant(),
                    typeService.getSuccessionCommand()));
        } else {
            db.addService(new ServicioInfo(date,
                    typeService.getId(),//typeId,
                    typeService.getName(),
                    typeService.getBgColor(),
                    typeService.getTextColor(),
                    typeService.getStartSchedule(),
                    typeService.getEndSchedule(),
                    typeService.getStartSchedule2(),
                    typeService.getEndSchedule2(),
                    typeService.getStartSchedule3(),
                    typeService.getEndSchedule3(),
                    typeService.getStartSchedule4(),
                    typeService.getEndSchedule4(),
                    "", 0,
                    typeService.getGuardiaCombinada(),
                    typeService.getTypeDay(),
                    0,
                    typeService.getSuccessionCommand()));
        }
    }

    /**
     * Cuando se abre el diálogo de saltar a
     */
    private DateSlider.OnDateSetListener mMonthYearSetListener =
            new DateSlider.OnDateSetListener() {
                public void onDateSet(DateSlider view, Calendar selectedDate) {
                    // update the mDateText view with the corresponding date
                    //Toast.makeText( getApplicationContext(), String.format("The chosen date:%n%tB %tY", selectedDate, selectedDate), Toast.LENGTH_LONG).show();
                    //month = selectedDate;
                    month.set(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), 1);
                    refreshCalendar();
                }
            };


    /**
     * Se llama siempre que se vuelve a ejecutar la página. Se ha incluido el
     * refresco del calendario.
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        //MyLog.d("onRestart", "onRestart");
        refreshCalendar();
    }

    /**
     * Refresca el calendario y el resumen
     */
    public void refreshCalendar() {
        baseRefreshCalendar(true);
    }

    /**
     * Refresca el calendario
     *
     * @param refresh_resume true si se quiere refrescar el resumen, false no.
     */
    public void refreshCalendar(boolean refresh_resume) {
        baseRefreshCalendar(refresh_resume);
    }

    private void baseRefreshCalendar(boolean refresh_resume) {
        //MyLog.d("refreshCalendar", "se ejecuta");
        TextView title = (TextView) findViewById(R.id.title);

        adapter.refreshDays();
        handler.post(calendarUpdater); // generate some random calendar items
        //adapter.notifyDataSetChanged();
        if (refresh_resume) refreshResume();

        title.setText(android.text.format.DateFormat.format("MMMM yyyy", month));
    }

    /**
     * Actualiza los dias del calendario
     */
    public Runnable calendarUpdater = new Runnable() {

        @Override
        public void run() {
            //MyLog.d("calendarUpdater", "run");
            items.clear();
            // format random values. You can implement a dedicated class to
            // provide real values
			/*
			for (int i = 0; i < 31; i++) {
				Random r = new Random();

				if (r.nextInt(10) > 6) {
					items.add(Integer.toString(i));
				}
			}
			*/
            //items.add(5, "COM");
            //Pas
			/*
			adapter.setPASActive(false);
			if(Sp.getPasActive(mContext)){
				int pas_year = Sp.getPasYear(mContext);
				int pas_week = Sp.getPasWeek(mContext);
				DateTime dt_pas = new DateTime().withYear(pas_year).withWeekOfWeekyear(pas_week);
				//miramos si el mes del calendario es posible ponerle el Pas
				//dependiendo si es de mayor fecha que el grabado de semana
				//en configuracion, si no, no nos interesa mostrar el Pas en 
				//las semanas anteriores a la actual
				if(month.get(Calendar.YEAR) > pas_year ||
						(month.get(Calendar.YEAR) == pas_year 
						&& month.get(Calendar.MONTH) >= dt_pas.getMonthOfYear() - 1)){
					adapter.setPASActive(true);
					adapter.setPASList(Sp.getPas(mContext));
					//adapter.notifyDataSetChanged();
				}
			}
			*/
            adapter.setItems(items);
            adapter.notifyDataSetChanged();
        }
    };


    public void refreshResume() {
        //MyLog.d("refreshResume", "refreshResume");
        // Cargamos el resumen del mes

        // List<TipoServicioInfo> tipos_servicios =
        // dsTipoServicio.getAllTipoServicios();
        LinearLayout layout_resume = (LinearLayout) findViewById(R.id.resume_month_services);
        //eliminamos todos los textview anteriores, xk cada vez que pasemos de mes
        //se verían encima de los del nuevo mes
        layout_resume.removeAllViews();
        //añadimos el textview donde pondrá la suma de horas totales del mes
        TextView txtTotalTime = new TextView(getApplicationContext());
        txtTotalTime.setId(-1);
        txtTotalTime.setTextColor(Cuadrante.SERVICE_DEFAULT_TEXT_COLOR);
        txtTotalTime.setText(Html.fromHtml("<b>" + getString(R.string.total_hours) + "</b>"));
        layout_resume.addView(txtTotalTime);
        //añadimos el textview donde pondrá la suma de horas trimestrales
        TextView txtQuarter = new TextView(getApplicationContext());
        txtQuarter.setId(-1);
        txtQuarter.setTextColor(Cuadrante.SERVICE_DEFAULT_TEXT_COLOR);
        txtQuarter.setText(Html.fromHtml("<b>" + getString(R.string.total_quarter_hours) + "</b>"));
        layout_resume.addView(txtQuarter);
        //añadimos el textview donde pondrá la suma de horas del f2
        TextView txtF2Hours = new TextView(getApplicationContext());
        txtF2Hours.setId(-1);
        txtF2Hours.setTextColor(Cuadrante.SERVICE_DEFAULT_TEXT_COLOR);
        txtF2Hours.setText(Html.fromHtml("<b>" + getString(R.string.total_f2_hours) + "</b>"));
        layout_resume.addView(txtF2Hours);
        //añadimos el textview donde pondrá la suma del F2
        TextView txtF2 = new TextView(getApplicationContext());
        txtF2.setId(-1);
        txtF2.setTextColor(Cuadrante.SERVICE_DEFAULT_TEXT_COLOR);
        txtF2.setText(Html.fromHtml("<b>" + getString(R.string.f2) + "</b>"));
        layout_resume.addView(txtF2);


        //dsTipoServicio = new TipoServicioDataSource(this);
        //dsTipoServicio.open();
        /**
         * Horas totales trabajadas, hours, minutes
         */
        HashMap<String, Integer> total_time = new HashMap<String, Integer>();
        total_time.put("hours", 0);
        total_time.put("minutes", 0);

        //db.destroyHoursTable();

        //obtenemos las horas del trimestre
        int actualMonth = month.get(Calendar.MONTH) + 1;
        HoursInfo actualMonthInfo = new HoursInfo();

        SparseArray<HoursInfo> quarterMonthsHours =
                db.getQuarterHours(month.get(Calendar.YEAR), actualMonth);

        List<Integer> months = CuadranteDates.getQuarterMonths(actualMonth);


        double totalReferenceHours = 0;
        double totalHours = 0;
        for (Integer m : months) {
            //MyLog.d(TAG, "----------------------------------");
            //MyLog.d(TAG, "m: " + m + " actualMonth:" + actualMonth);
            HoursInfo monthInfo = new HoursInfo();
            //MyLog.d(TAG, "mes:" + m);
            if (quarterMonthsHours.indexOfKey(m) < 0) {
                //MyLog.d(TAG, "el mes " + m + " no tiene horas guardadas");
                Calendar tmpMonth = (Calendar) month.clone();
                tmpMonth.set(Calendar.MONTH, m - 1);
                CalendarAdapter tmpAdapter = new CalendarAdapter(this, tmpMonth);
                //MyLog.d(TAG, tmpMonth.get(Calendar.MONTH) + " " + tmpMonth.get(Calendar.YEAR));
                monthInfo = setHoursInfo(tmpMonth, tmpAdapter);
            } else {
                monthInfo = quarterMonthsHours.get(m);
            }
            totalReferenceHours += monthInfo.getReference();
            totalHours += monthInfo.getHours();
            if (m == actualMonth) {
                actualMonthInfo = monthInfo;
            }
        }
        totalReferenceHours = Cuadrante.round(Cuadrante.doubleTwoDigits(totalReferenceHours));
		
		
		/*
		SparseArray<TipoServicioInfo> newTypeServices = new SparseArray<TipoServicioInfo>();
		//buscamos el tipo de servicio en la lista de tipos
		TipoServicioInfo ts = new TipoServicioInfo();
		for(TipoServicioInfo typeService:typeServices){
			newTypeServices.put(typeService.getId(), typeService);
		}
		ts = newTypeServices.get(dServices[i].getTypeId());
		*/
        ServicioInfo[] dServices = adapter.daysWithServices;
        List<TipoServicioInfo> typeServices = db.getAllTipoServicios();


        /**
         * Cantidad de veces que ha salido este mes un type_id
         * <type_id, qty>
         */
        SparseIntArray type_qty = new SparseIntArray();

        int qtyGuardias = 0;
        /**
         * Cantidad de veces que hay de cada tipo de dia
         * <type_day, qty>
         */
        SparseIntArray type_day_qty = new SparseIntArray();
        /**
         * Número de bajas médicas en el mes
         */
        int qtyMedicalLeave = 0;
        // miramos todos los dias del mes si tienen servicio
        for (int i = 0, qty = 0; i < dServices.length; i++) {

            //TipoServicioInfo ts = db.getTipoServicio(dServices[i].getTypeId());
            //buscamos el tipo de servicio en la lista de tipos
            TipoServicioInfo ts = new TipoServicioInfo();
            for (TipoServicioInfo typeService : typeServices) {
                if (typeService.getId() == dServices[i].getTypeId()) {
                    ts = typeService;
                    break;
                }
            }

            // si existe el type_id
            if (ts.getId() > 0) {
                // obtenemos el numero de veces que ha salido este tipo de
                // servicio este mes
                qty = type_qty.get(dServices[i].getTypeId(), 0);
                if (qty > 0) {// si es mayor que 0 significa que ya hay un textview
                    qty++;
                    TextView tv = (TextView) layout_resume
                            .findViewById(dServices[i].getTypeId());
                    tv.setText(ts.getTitle() + ": " + qty);
                } else {// no hay textview, asi que hay que crearlo
                    qty++;
                    TextView tv = new TextView(getApplicationContext());
                    tv.setId(dServices[i].getTypeId());
                    tv.setText(ts.getTitle() + ": " + qty);
                    tv.setBackgroundColor(Integer.parseInt(ts.getBgColor()));
                    tv.setTextColor(Integer.parseInt(ts.getTextColor()));
                    layout_resume.addView(tv);
                }
                type_qty.put(dServices[i].getTypeId(), qty);
                //si el tipo de servicio es el de bajas sumamos
                if (ts.getId() == Sp.getTypeServiceMedicalLeave(mContext)) qtyMedicalLeave++;
            }
            if (dServices[i].getGuardiaCombinada() > 0) qtyGuardias++;

        }
		/*
		//miramos si existen las vacaciones para insertar el credito que se ha gastado y el que
		//queda, pero solo para el año actual del movil
		TextView tvHolidays = 
				(TextView) layout_resume.findViewById(Sp.getHolidaysTypeService(mContext));
		DateTime dtToday = new DateTime();
		DateTime dtHolidaysStart = new DateTime();
		DateTime dtHolidaysEnd = new DateTime();
		Interval intervalHolidays = null;
		if(dtToday.getMonthOfYear() == 1){//empezamos el año pasado
			dtHolidaysStart = dtToday.withMonthOfYear(2).minusYears(1).withDayOfMonth(1);
			dtHolidaysEnd = dtToday.withDayOfMonth(31);
			intervalHolidays = new Interval(
					dtHolidaysStart,
					dtHolidaysEnd);
		}else{//este año para el siguiente
			dtHolidaysStart = dtToday.withDayOfMonth(1).withMonthOfYear(2);
			dtHolidaysEnd = dtToday.withMonthOfYear(1).plusYears(1).withDayOfMonth(31);
			intervalHolidays = new Interval(
					dtHolidaysStart,
					dtHolidaysEnd);			       	
		}		
		DateTime dtMonth = new DateTime(month);
		if(tvHolidays != null && intervalHolidays.overlaps(new Interval(dtMonth, dtMonth))){
			CuadranteDates cDS = new CuadranteDates(dtMonth.withDayOfMonth(1));
			CuadranteDates cDE = new CuadranteDates(dtMonth.dayOfMonth().withMaximumValue());
			List<ServicioInfo> services = db.getServicesFromTypeService(
					Sp.getHolidaysTypeService(this), 
					cDS.getDate(),
					cDE.getDate(), true, false);
			int numServicesSpent = Cuadrante.getDaysHolidaysSpent(services);
			tvHolidays.setText(
					String.format(
							getString(R.string.resume_holidays),
							tvHolidays.getText(),
							numServicesSpent
					)
			);			
		}
		*/


        double dif = actualMonthInfo.getHours() - actualMonthInfo.getReference();
        DecimalFormat dfH = new DecimalFormat("#.##");
        txtTotalTime.append(" " +
                String.format(
                        getString(R.string.resume_total_time),
                        dfH.format(actualMonthInfo.getHours()),
                        dfH.format(actualMonthInfo.getReference()),
                        dfH.format(dif)));
        dif = totalHours - totalReferenceHours;
        //txtQuarter.append(" " + dfH.format(totalHours) + " / " + dfH.format(totalReferenceHours) + " referencia / " + dfH.format(dif) + " diferencia");
        txtQuarter.append(" " +
                String.format(
                        getString(
                                R.string.resume_total_time),
                        dfH.format(totalHours),
                        dfH.format(totalReferenceHours),
                        dfH.format(dif)));
        txtF2Hours.append(" " + dfH.format(actualMonthInfo.getF2Hours()) + " / " + dfH.format(actualMonthInfo.getF2Percent()) + "%");
        if (qtyMedicalLeave > 5) //tachamos el f2
            txtF2.setPaintFlags(txtF2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        txtF2.append(" " + dfH.format(actualMonthInfo.getF2()) + " superior a 10.5");
        //int numAlerts = db.getGuardiasCombinadasServicesInMonth(month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1).size();
        if (qtyGuardias > 0) txtF2.append(" ó " + qtyGuardias + " de 4 guardias combinadas");

        //si sobrepasan algunos valores ponemos en rojo todo el texto
        if (actualMonthInfo.getHours() > actualMonthInfo.getReference()
                && actualMonthInfo.getReference() > 0)
            txtTotalTime.setTextColor(Cuadrante.SERVICE_DEFAULT_BG_COLOR_HOLIDAY);
        if (totalHours > totalReferenceHours && totalReferenceHours > 0)
            txtQuarter.setTextColor(Cuadrante.SERVICE_DEFAULT_BG_COLOR_HOLIDAY);
        if (actualMonthInfo.getF2() > 10.5)
            txtF2.setTextColor(Cuadrante.SERVICE_DEFAULT_BG_COLOR_HOLIDAY);
        if (qtyGuardias >= 4)
            txtF2.setTextColor(Cuadrante.SERVICE_DEFAULT_BG_COLOR_HOLIDAY);


        //revisamos si el mes actual del calendario es el mismo que el del
        //movil, si es asi entonces revisamos si tenemos que mostrar la
        //notificación del F2
        String today = CuadranteDates.formatDate(new DateTime().withDayOfMonth(1));
        String monthCalendar = CuadranteDates.formatDate(
                month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1, 1);
        //no podemos notificar el f2 si hay más de 5 bajas en este mes
        if (today.equals(monthCalendar) && qtyMedicalLeave <= 5) {
            //MyLog.d("refreshResume", "today:" + today + " - monthCalendar:" + monthCalendar);
            Notify.f2(this, db, actualMonthInfo.getF2(), qtyGuardias);
        }
    }

    public HoursInfo setHoursInfo(Calendar actualMonth, CalendarAdapter calendarAdapter) {
        /**
         * Horas totales trabajadas, hours, minutes
         */
        HashMap<String, Integer> total_time = new HashMap<String, Integer>();
        total_time.put("hours", 0);
        total_time.put("minutes", 0);

        HashMap<String, Double> f2_data = new HashMap<String, Double>();
        f2_data.put("indice", 0.0);
        f2_data.put("hours", 0.0);
        HashMap<String, Double> f2_data_tmp = new HashMap<String, Double>();

        //MyLog.d(TAG, "actualMonth:" + actualMonth.get(Calendar.MONTH));
        //miramos el servicio del ultimo dia del mes anterior por si tiene
        //horas pertenecientes al mes actual (22:00 - 06:00 pejem)
        int previousMonth = actualMonth.get(Calendar.MONTH);
        //previousMonth--;
        int previousYear = actualMonth.get(Calendar.YEAR);
        if (actualMonth.get(Calendar.MONTH) == 0) {
            previousMonth = 12;
            previousYear--;
        }
        //MyLog.d(TAG, "previousMonth:" + previousMonth);
        Calendar calendar = Calendar.getInstance();
        calendar.set(previousYear, previousMonth - 1, 1);
        //lastServicePreviousMonth
        ServicioInfo lSPM = db.getServicio(
                CuadranteDates.formatDate(
                        previousYear,
                        previousMonth,
                        calendar.getActualMaximum(Calendar.DAY_OF_MONTH)));
        if (lSPM.getId() > 0) {
            MyLog.d("refreshResume", "date previous service month:" + lSPM.getDate());
            Period day_period_previous = new Period();
            Period day_period_previous2 = new Period();
            Period day_period_previous3 = new Period();
            Period day_period_previous4 = new Period();
            if (!lSPM.getStartSchedule().equals(Cuadrante.SCHEDULE_NULL) ||
                    !lSPM.getEndSchedule().equals(Cuadrante.SCHEDULE_NULL) &&
                            lSPM.getTypeDay() == 0) {
                f2_data_tmp = Cuadrante.getHoursToF2(
                        db,
                        lSPM.getYear(),
                        lSPM.getMonth(),
                        lSPM.getDay(),
                        lSPM.getStartSchedule(),
                        lSPM.getEndSchedule(),
                        true);
                f2_data.put("indice", f2_data.get("indice") + f2_data_tmp.get("indice"));
                f2_data.put("hours", f2_data.get("hours") + f2_data_tmp.get("hours"));

                day_period_previous = Cuadrante.getTimeFromService(
                        lSPM.getYear(),
                        lSPM.getMonth(),
                        lSPM.getDay(),
                        lSPM.getStartSchedule(),
                        lSPM.getEndSchedule(),
                        true);
            }
            if (!lSPM.getStartSchedule2().equals(Cuadrante.SCHEDULE_NULL) ||
                    !lSPM.getEndSchedule2().equals(Cuadrante.SCHEDULE_NULL) &&
                            lSPM.getTypeDay() == 0) {
                f2_data_tmp = Cuadrante.getHoursToF2(
                        db,
                        lSPM.getYear(),
                        lSPM.getMonth(),
                        lSPM.getDay(),
                        lSPM.getStartSchedule2(),
                        lSPM.getEndSchedule2(),
                        true);
                f2_data.put("indice", f2_data.get("indice") + f2_data_tmp.get("indice"));
                f2_data.put("hours", f2_data.get("hours") + f2_data_tmp.get("hours"));
                day_period_previous2 = Cuadrante.getTimeFromService(
                        lSPM.getYear(),
                        lSPM.getMonth(),
                        lSPM.getDay(),
                        lSPM.getStartSchedule2(),
                        lSPM.getEndSchedule2(),
                        true);
            }
            if (!lSPM.getStartSchedule3().equals(Cuadrante.SCHEDULE_NULL) ||
                    !lSPM.getEndSchedule3().equals(Cuadrante.SCHEDULE_NULL) &&
                            lSPM.getTypeDay() == 0) {
                f2_data_tmp = Cuadrante.getHoursToF2(
                        db,
                        lSPM.getYear(),
                        lSPM.getMonth(),
                        lSPM.getDay(),
                        lSPM.getStartSchedule3(),
                        lSPM.getEndSchedule3(),
                        true);
                f2_data.put("indice", f2_data.get("indice") + f2_data_tmp.get("indice"));
                f2_data.put("hours", f2_data.get("hours") + f2_data_tmp.get("hours"));
                day_period_previous3 = Cuadrante.getTimeFromService(
                        lSPM.getYear(),
                        lSPM.getMonth(),
                        lSPM.getDay(),
                        lSPM.getStartSchedule3(),
                        lSPM.getEndSchedule3(),
                        true);
            }
            if (!lSPM.getStartSchedule4().equals(Cuadrante.SCHEDULE_NULL) ||
                    !lSPM.getEndSchedule4().equals(Cuadrante.SCHEDULE_NULL) &&
                            lSPM.getTypeDay() == 0) {
                f2_data_tmp = Cuadrante.getHoursToF2(
                        db,
                        lSPM.getYear(),
                        lSPM.getMonth(),
                        lSPM.getDay(),
                        lSPM.getStartSchedule4(),
                        lSPM.getEndSchedule4(),
                        true);
                f2_data.put("indice", f2_data.get("indice") + f2_data_tmp.get("indice"));
                f2_data.put("hours", f2_data.get("hours") + f2_data_tmp.get("hours"));
                day_period_previous4 = Cuadrante.getTimeFromService(
                        lSPM.getYear(),
                        lSPM.getMonth(),
                        lSPM.getDay(),
                        lSPM.getStartSchedule4(),
                        lSPM.getEndSchedule4(),
                        true);
            }
            int hours = day_period_previous.getHours() + day_period_previous2.getHours() +
                    day_period_previous3.getHours() + day_period_previous4.getHours();
            int minutes = day_period_previous.getMinutes() +
                    day_period_previous2.getMinutes() + day_period_previous3.getMinutes() +
                    day_period_previous4.getMinutes();

            total_time.put("hours", hours);
            total_time.put("minutes", minutes);
            //MyLog.d("refreshResume", "time previous service month:" + hours + " " + minutes);
        }

        ServicioInfo[] dServices = calendarAdapter.daysWithServices;


        /**
         * Cantidad de veces que ha salido este mes un type_id
         * <type_id, qty>
         */
        SparseIntArray type_qty = new SparseIntArray();

        int qtyGuardias = 0;
        //MyLog.d(TAG, "last service: " + hoursToF2);
        /**
         * Cantidad de veces que hay de cada tipo de dia
         * <type_day, qty>
         */
        SparseIntArray type_day_qty = new SparseIntArray();
        // miramos todos los dias del mes si tienen servicio
        for (int i = 0; i < dServices.length; i++) {
            int qty = 0;
            Period day_period = new Period();
            Period day_period2 = new Period();
            Period day_period3 = new Period();
            Period day_period4 = new Period();
            if (dServices[i].getId() > 0) {// el dia tiene servicio
                //si es guardia significa que no hay que sumar las horas de los horarios
                //si no lo que tiene cada tipo de guardia
                boolean isGuardia = false;
                /**
                 * Computo de tiempos de servicio.
                 A efectos de determinar la superación del tiempo de referencia para el
                 cálculo de sobreesfuerzos, las guardias combinadas computarán:
                  Clases A, C y D: 7 horas, con independencia de que se haya llegado o
                 no a requerir la presencia del perceptor durante el período de
                 localización.
                  Clase B: 16 horas, con igual criterio respecto de la presencia del
                 perceptor.
                  Clase E y F: tendrán la consideración de días deducibles.
                 */
                switch (dServices[i].getGuardiaCombinada()) {
                    case 1://A
                    case 3://C
                    case 4://D
                        day_period = day_period.plusHours(7);
                        isGuardia = true;
                        MyLog.d(TAG, day_period.getHours());
                        break;
                    case 2://B
                        day_period = day_period.plusHours(16);
                        isGuardia = true;
                        break;
                    default:
                        break;
                }

                if (!dServices[i].getStartSchedule().equals(Cuadrante.SCHEDULE_NULL) &&
                        !dServices[i].getEndSchedule().equals(Cuadrante.SCHEDULE_NULL) &&
                        dServices[i].getTypeDay() == 0) {
                    //MyLog.d("main", "dentro de schedule 1 :"+ dServices[i].getStartSchedule());
                    //horas para conseguir el F2
                    f2_data_tmp = Cuadrante.getHoursToF2(
                            db,
                            dServices[i].getYear(),
                            dServices[i].getMonth(),
                            dServices[i].getDay(),
                            dServices[i].getStartSchedule(),
                            dServices[i].getEndSchedule());
                    f2_data.put("indice", f2_data.get("indice") + f2_data_tmp.get("indice"));
                    f2_data.put("hours", f2_data.get("hours") + f2_data_tmp.get("hours"));
                    MyLog.d(TAG, "dia total hours f2:" + f2_data.get("hours"));
                    if (!isGuardia) {
                        // horas totales
                        day_period = Cuadrante.getTimeFromService(
                                dServices[i].getYear(),
                                dServices[i].getMonth(),
                                dServices[i].getDay(),
                                dServices[i].getStartSchedule(),
                                dServices[i].getEndSchedule());
                    }
                }
                if (!dServices[i].getStartSchedule2().equals(Cuadrante.SCHEDULE_NULL) &&
                        !dServices[i].getEndSchedule2().equals(Cuadrante.SCHEDULE_NULL) &&
                        dServices[i].getTypeDay() == 0) {
                    //MyLog.d("main", "dentro de schedule 2 :"+ dServices[i].getStartSchedule2());
                    f2_data_tmp = Cuadrante.getHoursToF2(
                            db,
                            dServices[i].getYear(),
                            dServices[i].getMonth(),
                            dServices[i].getDay(),
                            dServices[i].getStartSchedule2(),
                            dServices[i].getEndSchedule2());
                    f2_data.put("indice", f2_data.get("indice") + f2_data_tmp.get("indice"));
                    f2_data.put("hours", f2_data.get("hours") + f2_data_tmp.get("hours"));
                    if (!isGuardia) {
                        day_period2 = Cuadrante.getTimeFromService(
                                dServices[i].getYear(),
                                dServices[i].getMonth(),
                                dServices[i].getDay(),
                                dServices[i].getStartSchedule2(),
                                dServices[i].getEndSchedule2());
                    }
                }
                if (!dServices[i].getStartSchedule3().equals(Cuadrante.SCHEDULE_NULL) &&
                        !dServices[i].getEndSchedule3().equals(Cuadrante.SCHEDULE_NULL) &&
                        dServices[i].getTypeDay() == 0) {
                    //MyLog.d("main", "dentro de schedule 3 :"+ dServices[i].getStartSchedule3());
                    f2_data_tmp = Cuadrante.getHoursToF2(
                            db,
                            dServices[i].getYear(),
                            dServices[i].getMonth(),
                            dServices[i].getDay(),
                            dServices[i].getStartSchedule3(),
                            dServices[i].getEndSchedule3());
                    f2_data.put("indice", f2_data.get("indice") + f2_data_tmp.get("indice"));
                    f2_data.put("hours", f2_data.get("hours") + f2_data_tmp.get("hours"));
                    if (!isGuardia) {
                        day_period3 = Cuadrante.getTimeFromService(
                                dServices[i].getYear(),
                                dServices[i].getMonth(),
                                dServices[i].getDay(),
                                dServices[i].getStartSchedule3(),
                                dServices[i].getEndSchedule3());
                    }
                }
                if (!dServices[i].getStartSchedule4().equals(Cuadrante.SCHEDULE_NULL) &&
                        !dServices[i].getEndSchedule4().equals(Cuadrante.SCHEDULE_NULL) &&
                        dServices[i].getTypeDay() == 0) {
                    //MyLog.d("main", "dentro de schedule 4 :"+ dServices[i].getStartSchedule4());
                    f2_data_tmp = Cuadrante.getHoursToF2(
                            db,
                            dServices[i].getYear(),
                            dServices[i].getMonth(),
                            dServices[i].getDay(),
                            dServices[i].getStartSchedule4(),
                            dServices[i].getEndSchedule4());
                    f2_data.put("indice", f2_data.get("indice") + f2_data_tmp.get("indice"));
                    f2_data.put("hours", f2_data.get("hours") + f2_data_tmp.get("hours"));
                    if (!isGuardia) {
                        day_period4 = Cuadrante.getTimeFromService(
                                dServices[i].getYear(),
                                dServices[i].getMonth(),
                                dServices[i].getDay(),
                                dServices[i].getStartSchedule4(),
                                dServices[i].getEndSchedule4());
                    }
                }
                //tipo de dia, 1 -> 5.35, 2 -> 7.5, 3 -> -1 día al mes
                //para que si el usuario pone un dia como de baja pero a la vez es festivo pues
                //que no reste los dos horarios al computo de referencia
                if (dServices[i].getTypeDay() > 0) {
                    qty = type_day_qty.get(dServices[i].getTypeDay(), 0);
                    qty++;
                    type_day_qty.put(dServices[i].getTypeDay(), qty);
                    //tipo de dia festivo, lo ponemos como 0 porque no existe el 0 en tipo de dia
                } else if (dServices[i].getIsHoliday() == 1) {
                    qty = type_day_qty.get(0, 0);
                    //MyLog.d("refreshResume", "type_day_qty:" + type_day_qty.get(0, 0));
                    qty++;
                    type_day_qty.put(0, qty);
                    //MyLog.d("refreshResume", "festivo:" + dServices[i].getDay() + " " + dServices[i].getIsHoliday() + " qty:" + qty);
                }

                int hours = day_period.getHours() + day_period2.getHours() +
                        day_period3.getHours() + day_period4.getHours();
                int minutes = day_period.getMinutes() +
                        day_period2.getMinutes() + day_period3.getMinutes() +
                        day_period4.getMinutes();

                //días en sucesión de mando
                if (dServices[i].getSuccessionCommand() > 0) {
                    if (dServices[i].getSuccessionCommand() == 1) {//E12
                        f2_data.put("indice", f2_data.get("indice") + 1.75);
                        //hoursToF2 += 1.75;
                    } else if (dServices[i].getSuccessionCommand() == 2) {//E13
                        f2_data.put("indice", f2_data.get("indice") + 2);
                        //hoursToF2 += 2;
                    }
                }

                if (total_time.containsKey("hours")) {
                    total_time.put("hours", total_time.get("hours") + hours);
                    total_time.put("minutes", total_time.get("minutes") + minutes);
                } else {
                    total_time.put("hours", hours);
                    total_time.put("minutes", minutes);
                }

                if (dServices[i].getGuardiaCombinada() > 0) {
                    qtyGuardias++;
                }
            }
        }


        //miramos si los minutos se pueden pasar a horas
        if (total_time.containsKey("minutes")) {
            if (total_time.get("minutes") >= 60) {
                //sumamos la división entre los minutos y 60
                total_time.put("hours", total_time.get("hours") + total_time.get("minutes") / 60);
                //el resto de minutos los ponemos como minutos
                total_time.put("minutes", total_time.get("minutes") % 60);
            }
        }

        //obtenemos las horas de referencia mensuales
        double reference_hours = Cuadrante.getReferenceHours(
                actualMonth.getActualMaximum(Calendar.DAY_OF_MONTH) - type_day_qty.get(3, 0),
                type_day_qty.get(1, 0),
                type_day_qty.get(2, 0),
                type_day_qty.get(0, 0),
                Sp.getComputingHoursPerWeek(this));


        //txTotalTime.append(" " + total_time.get("hours") + " horas y " + total_time.get("minutes") + " minutos");
        int minutes = (total_time.get("minutes") * 100) / 60;

        //MyLog.d(TAG, "******final hours f2:" + f2_data.get("hours"));
        //MyLog.d(TAG, "******final hours mes:" + Double.parseDouble(total_time.get("hours") + "." + minutes));

        HoursInfo hoursInfo = new HoursInfo(
                actualMonth.get(Calendar.YEAR),
                actualMonth.get(Calendar.MONTH) + 1,
                Double.parseDouble(total_time.get("hours") + "." + minutes),
                reference_hours,
                f2_data.get("indice"),
                qtyGuardias,
                f2_data.get("hours"));

        //MyLog.d(TAG, "mes: " + hoursInfo.getMonth());
        //MyLog.d(TAG, "horas: " + Double.parseDouble(total_time.get("hours") + "." + minutes));
        //MyLog.d(TAG, "f2: " + hoursToF2);
        //MyLog.d(TAG, "year:" + hoursInfo.getYear() + " month:" + hoursInfo.getMonth());
        //MyLog.d(TAG, "hours:" + hoursInfo.getHours() + " reference:" + hoursInfo.getReference() + " f2:" + hoursInfo.getF2());
        //MyLog.d(TAG, "guarcias:" + hoursInfo.getGuardias());
        db.insertHours(hoursInfo);

        return hoursInfo;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // MyLog.d("CheckStartActivity","onActivityResult and resultCode = "+resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        //MyLog.d("Main onActivityResult", "resultCode:" + resultCode + " RESULT_OK:" + RESULT_OK);


        //para que nos refresque el calendario si venimos tambien de la página de configuración
        if (resultCode == RESULT_OK) {
            refreshCalendar();
        }
        //debemos de mostrar el datePicker de selección de rango de fechas
        //porque se viene de crear un tipo de servicio nuevo para un dia
        //del calendario, y este tipo tiene rango de fechas
        if (requestCode == Cuadrante.REQUEST_CODE_CREATE_TYPE && resultCode > 0) {
            this.typeId = resultCode;
            showDialog(DIALOG_ID_TYPE_SERVICE_DATE_RANGE);
        }


        //si le das al boton de atrás o al botón "Cancelar" y se vuelve a
        //este activi el resultCode es 0, por lo que mejor SIEMPRE es refrescar
        //el calendario
        // TODO 28-10-12 poniendo el refresco en onRestart, SIEMPRE lo hará cuando se vuelva a mostrar el MainActivity
        //refreshCalendar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Cuadrante.checkSignIn(this);
    }
}