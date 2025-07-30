import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminMatchComponent } from './admin-match.component';

describe('AdminMatchComponent', () => {
  let component: AdminMatchComponent;
  let fixture: ComponentFixture<AdminMatchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminMatchComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminMatchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
