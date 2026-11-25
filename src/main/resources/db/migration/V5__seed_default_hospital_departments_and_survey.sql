-- Default hospital departments (idempotent; preserves any existing departments)
INSERT INTO departments (name, active)
SELECT seed.name, true
FROM (
    VALUES
        ('Acil Servis'),
        ('Poliklinik'),
        ('Yatan Hasta'),
        ('Laboratuvar'),
        ('Radyoloji'),
        ('Kadın Doğum'),
        ('Çocuk Sağlığı ve Hastalıkları'),
        ('Ameliyathane ve Yoğun Bakım')
) AS seed(name)
WHERE NOT EXISTS (
    SELECT 1 FROM departments d WHERE d.name = seed.name
);

-- Default ready-to-use hospital satisfaction survey (only seeded if no survey exists yet)
INSERT INTO surveys (title, active, created_at)
SELECT 'Hüma Genel Hasta Memnuniyet Anketi', true, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM surveys
);

-- Ensure a published v1 version exists for the default survey
INSERT INTO survey_versions (survey_id, version_number, status, created_at, published_at)
SELECT s.id, 1, 'PUBLISHED', COALESCE(s.created_at, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP
FROM surveys s
WHERE s.title = 'Hüma Genel Hasta Memnuniyet Anketi'
  AND NOT EXISTS (
      SELECT 1 FROM survey_versions sv WHERE sv.survey_id = s.id
  );

-- Seed COMMON questions into the published v1 version of the default survey
INSERT INTO questions (
    survey_id,
    survey_version_id,
    text,
    type,
    scope,
    department_id,
    metric_code,
    display_order,
    required,
    active
)
SELECT
    sv.survey_id,
    sv.id,
    common.text,
    common.type,
    'COMMON',
    NULL,
    common.metric_code,
    common.display_order,
    common.required,
    true
FROM survey_versions sv
JOIN surveys s ON s.id = sv.survey_id
CROSS JOIN (
    VALUES
        ('Personelin iletişiminden memnun kaldınız mı?', 'RATING', 'COMMUNICATION', 10, true),
        ('Size sunulan hizmet hakkında yeterince bilgilendirildiniz mi?', 'RATING', 'INFORMATION', 20, true),
        ('Hizmet sürecinde ihtiyaçlarınıza yeterince özen gösterildi mi?', 'RATING', 'SERVICE_ATTENTION', 30, true),
        ('Genel hizmet deneyiminizi nasıl değerlendirirsiniz?', 'RATING', 'GENERAL_EXPERIENCE', 40, true),
        ('Eklemek istediğiniz görüş veya öneriniz var mı?', 'TEXT', 'GENERAL_COMMENT', 900, false)
) AS common(text, type, metric_code, display_order, required)
WHERE s.title = 'Hüma Genel Hasta Memnuniyet Anketi'
  AND sv.status = 'PUBLISHED'
  AND NOT EXISTS (
      SELECT 1
      FROM questions q
      WHERE q.survey_id = sv.survey_id
        AND q.survey_version_id = sv.id
        AND q.scope = 'COMMON'
        AND q.metric_code = common.metric_code
  );

-- Seed DEPARTMENT_SPECIFIC questions using exact Turkish department names
INSERT INTO questions (
    survey_id,
    survey_version_id,
    text,
    type,
    scope,
    department_id,
    metric_code,
    display_order,
    required,
    active
)
SELECT
    sv.survey_id,
    sv.id,
    template.text,
    'RATING',
    'DEPARTMENT_SPECIFIC',
    d.id,
    template.metric_code,
    template.display_order,
    true,
    true
FROM survey_versions sv
JOIN surveys s ON s.id = sv.survey_id
JOIN departments d ON d.active = true
CROSS JOIN (
    VALUES
        ('Acil Servis', 'İlk müdahale süresi beklentilerinizi karşıladı mı?', 'FIRST_RESPONSE_TIME', 110),
        ('Acil Servis', 'Acil servis süreçlerini nasıl değerlendirirsiniz?', 'EMERGENCY_PROCESS', 120),
        ('Poliklinik', 'Randevu sürecinden memnun kaldınız mı?', 'APPOINTMENT_PROCESS', 110),
        ('Poliklinik', 'Bekleme süresini nasıl değerlendirirsiniz?', 'WAITING_TIME', 120),
        ('Poliklinik', 'Muayene deneyiminizi nasıl değerlendirirsiniz?', 'EXAMINATION_EXPERIENCE', 130),
        ('Yatan Hasta', 'Hemşirelik bakımından memnun kaldınız mı?', 'NURSING_CARE', 110),
        ('Yatan Hasta', 'Oda konforunu nasıl değerlendirirsiniz?', 'ROOM_COMFORT', 120),
        ('Yatan Hasta', 'Temizlikten memnun kaldınız mı?', 'CLEANLINESS', 130),
        ('Laboratuvar', 'Numune alma sürecini nasıl değerlendirirsiniz?', 'SAMPLE_COLLECTION', 110),
        ('Laboratuvar', 'Sonuçlara erişim sürecini nasıl değerlendirirsiniz?', 'RESULT_ACCESS', 120),
        ('Laboratuvar', 'Laboratuvar bekleme süresini nasıl değerlendirirsiniz?', 'WAITING_TIME', 130),
        ('Radyoloji', 'Görüntüleme sürecini nasıl değerlendirirsiniz?', 'IMAGING_PROCESS', 110),
        ('Radyoloji', 'İşlem öncesi bilgilendirmeden memnun kaldınız mı?', 'PROCEDURE_INFORMATION', 120),
        ('Radyoloji', 'Radyoloji bekleme süresini nasıl değerlendirirsiniz?', 'WAITING_TIME', 130)
) AS template(department_name, text, metric_code, display_order)
WHERE s.title = 'Hüma Genel Hasta Memnuniyet Anketi'
  AND sv.status = 'PUBLISHED'
  AND d.name = template.department_name
  AND NOT EXISTS (
      SELECT 1
      FROM questions q
      WHERE q.survey_id = sv.survey_id
        AND q.survey_version_id = sv.id
        AND q.department_id = d.id
        AND q.metric_code = template.metric_code
  );
