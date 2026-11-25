CREATE UNIQUE INDEX usuarios_email_sem_caixa_uk ON usuarios (lower(email));
